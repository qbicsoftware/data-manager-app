package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Span;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Code Block
 * <p>
 * Inline Code Block component based on the {@link Span} component, which allows the user to copy
 * the code shown within the block
 */
public class CodeBlock extends Span {

  public CodeBlock(String command, String... parameters) {
    String collectedParameters = Arrays.stream(parameters).map(parameter -> parameter + " ")
        .collect(
            Collectors.joining());
    String codeToBeCopied = String.format("%s %s", command, collectedParameters);
    String codeBlockText = "$ " + codeToBeCopied;
    Span codeBlockTextSpan = new Span(codeBlockText);
    CopyToClipBoardComponent copyToClipBoardComponent = new CopyToClipBoardComponent(codeToBeCopied);
    add(codeBlockTextSpan, copyToClipBoardComponent);
    addClassName("code-block");
  }

}
