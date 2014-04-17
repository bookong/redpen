/**
 * redpen: a text inspection tool
 * Copyright (C) 2014 Recruit Technologies Co., Ltd. and contributors
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
package org.unigram.docvalidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unigram.docvalidator.config.CharacterTable;
import org.unigram.docvalidator.config.DVResource;
import org.unigram.docvalidator.config.ValidatorConfiguration;
import org.unigram.docvalidator.distributor.DefaultResultDistributor;
import org.unigram.docvalidator.distributor.ResultDistributor;
import org.unigram.docvalidator.distributor.ResultDistributorFactory;
import org.unigram.docvalidator.formatter.Formatter;
import org.unigram.docvalidator.model.Document;
import org.unigram.docvalidator.model.DocumentCollection;
import org.unigram.docvalidator.model.Paragraph;
import org.unigram.docvalidator.model.Section;
import org.unigram.docvalidator.model.Sentence;
import org.unigram.docvalidator.validator.SentenceIterator;
import org.unigram.docvalidator.validator.Validator;
import org.unigram.docvalidator.validator.section.ParagraphNumberValidator;
import org.unigram.docvalidator.validator.section.ParagraphStartWithValidator;
import org.unigram.docvalidator.validator.section.SectionLengthValidator;
import org.unigram.docvalidator.validator.section.SectionValidator;
import org.unigram.docvalidator.validator.sentence.SentenceValidator;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Validate all input files using appended Validators.
 */
public class DocumentValidator implements Validator {

  private final List<SectionValidator> sectionValidators;

  private final List<SentenceValidator> sentenceValidators;

  private DocumentValidator(Builder builder) throws DocumentValidatorException {
    DVResource resource = builder.resource;
    this.distributor = builder.distributor;
    this.conf = resource.getConfiguration();
    this.charTable = resource.getCharacterTable();

    validators = new ArrayList<Validator>();
    sectionValidators = new ArrayList<SectionValidator>();
    sentenceValidators = new ArrayList<SentenceValidator>();

    loadValidators(this.conf, this.charTable);
  }

  /**
   * Load validators written in the configuration file.
   *
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private void loadValidators(ValidatorConfiguration rootConfig,
                              CharacterTable charTable) throws
    DocumentValidatorException {

    for (ValidatorConfiguration config : rootConfig.getChildren()) {
      loadValidator(charTable, config);
    }
  }

  private void loadValidator(CharacterTable charTable, ValidatorConfiguration
    config) throws DocumentValidatorException {
    String confName = config.getConfigurationName();

    if (confName.equals("SentenceIterator")) {
      validators.add(new SentenceIterator(config, charTable));
    } else if (confName.equals("SectionLength")) {
      sectionValidators.add(new SectionLengthValidator(config, charTable));
    } else if (confName.equals("MaxParagraphNumber")) {
      sectionValidators.add(new ParagraphNumberValidator(config, charTable));
    } else if (confName.equals("ParagraphStartWith")) {
      sectionValidators.add(new ParagraphStartWithValidator(config, charTable));
    } else {
      throw new DocumentValidatorException(
        "There is no Validator like " + confName);
    }
  }

  /**
   * Validate the input document collection.
   *
   * @param documentCollection input document collection generated by Parser
   * @return list of validation errors
   */
  public List<ValidationError> check(DocumentCollection documentCollection) {
    distributor.flushHeader();
    List<ValidationError> errors = new ArrayList<ValidationError>();
//    for (Validator validator : this.validators) {


//      Iterator<Document> fileIterator = documentCollection.getDocuments();
//      while (fileIterator.hasNext()) {
//        try {
//          List<ValidationError> currentErrors =
//            validator.validate(fileIterator.next());
//          errors.addAll(currentErrors);
//        } catch (Throwable e) {
//          LOG.error("Error occurs in validation: " + e.getMessage());
//          LOG.error("Validator class: " + validator.getClass());
//        }
//      }
//    }

    for (Document document : documentCollection) {
      errors = validateDocument(document);
    }

    distributor.flushFooter();
    return errors;
  }

  private List<ValidationError> validateDocument(Document document) {
    List<ValidationError> errors = new ArrayList<ValidationError>();
    for (Validator validator : validators) {
      errors.addAll(validator.validate(document));
    }

    for (Section section : document) {
      errors.addAll(validateSection(section));
    }
    return errors;
  }

  private List<ValidationError> validateSection(Section section) {
    List<ValidationError> errors = new ArrayList<ValidationError>();
    for (SectionValidator sectionValidator : sectionValidators) {
      errors.addAll(sectionValidator.validate(section));
    }

    for (Paragraph paragraph : section.getParagraphs()) {
      errors.addAll(validateParagraph(paragraph));
    }
    return errors;
  }

  private List<ValidationError> validateParagraph(Paragraph paragraph) {
    List<ValidationError> errors = new ArrayList<ValidationError>();
    for (SentenceValidator sentenceValidator : sentenceValidators) {
      for (Sentence sentence : paragraph.getSentences()) {
        errors.addAll(sentenceValidator.validate(sentence));
      }
    }
    return errors;
  }

  /**
   * Constructor only for testing.
   */
  protected DocumentValidator() {
    this.distributor = ResultDistributorFactory
        .createDistributor(Formatter.Type.PLAIN,
            System.out);
    this.validators = new ArrayList<Validator>();
    sectionValidators = new ArrayList<SectionValidator>();
    sentenceValidators = new ArrayList<SentenceValidator>();
    this.conf = null;
    this.charTable = null;
  }

  /**
   * Append a specified validator.
   *
   * @param validator Validator used in testing
   */
  protected void appendValidator(Validator validator) {
    this.validators.add(validator);
  }

  @Override
  public List<ValidationError> validate(Document document) {
    return null;
  }

  public void appendSectionValidator(SectionValidator validator) {
    sectionValidators.add(validator);
  }

  public static class Builder {

    private DVResource resource;

    private ResultDistributor distributor = new DefaultResultDistributor(
      new PrintStream(System.out)
    );

    public Builder setResource(DVResource resource) {
      this.resource = resource;
      return this;
    }

    public Builder setResultDistributor(ResultDistributor distributor) {
      this.distributor = distributor;
      return this;
    }

    public DocumentValidator build() throws DocumentValidatorException {
      return new DocumentValidator(this);
    }
  }

  private final List<Validator> validators;

  private final ValidatorConfiguration conf;

  private final CharacterTable charTable;

  private ResultDistributor distributor;

  private static final Logger LOG =
    LoggerFactory.getLogger(DocumentValidator.class);
}
