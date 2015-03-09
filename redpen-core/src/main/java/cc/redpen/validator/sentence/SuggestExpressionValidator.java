/**
 * redpen: a text inspection tool
 * Copyright (c) 2014-2015 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.redpen.validator.sentence;

import cc.redpen.RedPenException;
import cc.redpen.model.Sentence;
import cc.redpen.util.KeyValueDictionaryExtractor;
import cc.redpen.validator.CloneableValidator;
import cc.redpen.validator.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * If input sentences contain invalid expressions, this validator
 * returns the errors with corrected expressions.
 */
final public class SuggestExpressionValidator extends CloneableValidator {

    private static final Logger LOG =
            LoggerFactory.getLogger(SuggestExpressionValidator.class);
    private static final String DEFAULT_RESOURCE_PATH = "";
    private Map<String, String> synonyms = new HashMap<>();

    @Override
    public void validate(List<ValidationError> errors, Sentence sentence) {
        synonyms.keySet().stream().forEach(value -> {
                    int startPosition = sentence.getContent().indexOf(value);
                    if (startPosition != -1) {
                        errors.add(createValidationErrorWithPosition(sentence,
                                sentence.getOffset(startPosition),
                                sentence.getOffset(startPosition + value.length()),
                                synonyms.get(value)));
                    }
                }
        );
    }

    @Override
    protected void init() throws RedPenException {
        //TODO: support default dictionary.
        Optional<String> confFile = getConfigAttribute("dict");
        LOG.info("Dictionary file is " + confFile);
        if (!confFile.isPresent()) {
            LOG.error("Dictionary file is not specified");
            throw new RedPenException("dictionary file is not specified");
        } else {
            KeyValueDictionaryExtractor extractor = new KeyValueDictionaryExtractor();
            try {
                extractor.load(new FileInputStream(confFile.get()));
            } catch (IOException e) {
                throw new RedPenException("Failed to load KeyValueDictionaryExtractor", e);
            }
            synonyms = extractor.get();
        }
    }

    protected void setSynonyms(Map<String, String> synonymMap) {
        this.synonyms = synonymMap;
    }

    @Override
    public String toString() {
        return "SuggestExpressionValidator{" +
                "synonyms=" + synonyms +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuggestExpressionValidator that = (SuggestExpressionValidator) o;

        return !(synonyms != null ? !synonyms.equals(that.synonyms) : that.synonyms != null);

    }

    @Override
    public int hashCode() {
        return synonyms != null ? synonyms.hashCode() : 0;
    }
}
