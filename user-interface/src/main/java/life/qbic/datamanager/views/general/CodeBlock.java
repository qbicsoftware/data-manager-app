package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Code Block
 * <p>
 * Inline Code Block component based on the {@link Span} component, which allows the user to copy
 * the code shown within the block
 */
@JsModule("./javascript/copytoclipboard.js")
public class CodeBlock extends Span {

  public CodeBlock(String command, String... parameters) {
    String collectedParameters = Arrays.stream(parameters).map(parameter -> parameter + " ")
        .collect(
            Collectors.joining());
    String codeToBeCopied = String.format("%s %s", command, collectedParameters);
    String codeBlockText = "$ " + codeToBeCopied;
    Span codeBlockTextSpan = new Span(codeBlockText);
    add(codeBlockTextSpan, generateCopyIcon(codeToBeCopied));
    addClassName("code-block");
  }

  private static Icon generateCopyIcon(String copyContent) {
    Icon copyIcon = VaadinIcon.COPY_O.create();
    copyIcon.addClassNames("clickable", "copy-icon");
    copyIcon.addClickListener(
        event -> UI.getCurrent().getPage().executeJs("window.copyToClipboard($0)", copyContent));
    return copyIcon;
  }
}
