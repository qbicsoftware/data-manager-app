package life.qbic.datamanager.files.export;

import java.util.Collections;
import java.util.List;
import life.qbic.datamanager.files.export.FileSupplier.FormatException;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

@FunctionalInterface
public interface DocxBuilder<T> {


  record Section(String title, List<?> content) {

    public Object titleAsParagraph(MainDocumentPart mainDocumentPart) {
      return mainDocumentPart.createStyledParagraphOfText("Heading1", title);
    }

    @Override
    public List<?> content() {
      return Collections.unmodifiableList(content);
    }
  }

  private static void addSection(MainDocumentPart mainDocumentPart, Section section) {
    mainDocumentPart.addObject(section.titleAsParagraph(mainDocumentPart));
    section.content().forEach(mainDocumentPart::addObject);
  }

  static Object createTitle(MainDocumentPart mainDocumentPart, String title) {
    return mainDocumentPart.createStyledParagraphOfText("Title", title);
  }

  static Object createSubtitle(MainDocumentPart mainDocumentPart, String subTitle) {
    return mainDocumentPart.createStyledParagraphOfText("Subtitle", subTitle);
  }


  List<Object> createContent(MainDocumentPart mainDocumentPart, T input);

  default WordprocessingMLPackage buildFrom(T payload) {
    try {
      var result = WordprocessingMLPackage.createPackage();
      var mainDocument = result.getMainDocumentPart();
      List<Object> content = createContent(mainDocument, payload);
      for (Object part : content) {
        if (part instanceof Section section) {
          addSection(mainDocument, section);
        } else {
          mainDocument.addObject(part);
        }
      }
      return result;
    } catch (Docx4JException e) {
      throw new FormatException("Creating docx package failed. ", e);
    }
  }
}
