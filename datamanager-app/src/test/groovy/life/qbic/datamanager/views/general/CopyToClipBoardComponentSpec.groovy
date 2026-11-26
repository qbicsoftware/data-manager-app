package life.qbic.datamanager.views.general

import spock.lang.Specification

/**
 * Guards the copy control against the defect that made it one-shot.
 *
 * <p>The control hides the copy icon and shows a success icon for a moment after a click. The
 * reset was scheduled with {@code UI.accessLater}, which defers a task until the <em>currently
 * executing request</em> finishes — called from a delayed background thread there is no current
 * request, so the task was queued for a request that never arrived. The success icon stayed
 * forever and the copy icon never returned, so after its first use the control looked dead.</p>
 *
 * <p>This class is shared: the code blocks of the download guidance, the ontology lookup and the
 * personal access token view all use it. A regression silently breaks all three, and could only be
 * observed by clicking twice, so the rule is checked against the source.</p>
 */
class CopyToClipBoardComponentSpec extends Specification {

    private static String source() {
        def candidates = [
                new File("datamanager-app/src/main/java/life/qbic/datamanager/views/general/CopyToClipBoardComponent.java"),
                new File("src/main/java/life/qbic/datamanager/views/general/CopyToClipBoardComponent.java"),
                new File("../datamanager-app/src/main/java/life/qbic/datamanager/views/general/CopyToClipBoardComponent.java")
        ]
        def file = candidates.find { it.exists() }
        assert file != null: "CopyToClipBoardComponent.java not found from ${new File('.').absolutePath}"
        return file.text
    }

    /** The source with comments removed, so prose about a pattern cannot satisfy a rule. */
    private static String code() {
        String withoutBlocks = source().replaceAll('(?s)/\\*.*?\\*/', '')
        return withoutBlocks.replaceAll('(?m)^\\s*//.*$', '')
    }

    def "the scheduled reset re-enters the UI through access, not accessLater"() {
        expect: "accessLater defers to the end of a request, and a delayed thread has no request"
        !code().contains("accessLater")
        code().contains("ui.access(")
    }

    def "the copy icon is restored after a click"() {
        expect: "the control must be usable again, not only once"
        code().contains("showCopyIcon()")
        code().contains("copyIcon.setVisible(true)")
        code().contains("copySuccessIcon.setVisible(false)")
    }

    def "a second click cancels the first click's pending reset"() {
        expect: "two timers racing to set the same two icons would flicker or strand the state"
        code().contains("cancelPendingReset()")
    }

    def "the pending reset is cancelled when the component detaches"() {
        expect: "a detached component must not be written to from a timer"
        code().contains("onDetach")
        code().contains("cancelPendingReset()")
    }

    def "the confirmation is shown before the reset is scheduled"() {
        given: "only the click handler, not the whole file — the helper is also declared elsewhere"
        def body = code()
        int handlerStart = body.indexOf("private void handleCopyClicked")
        assert handlerStart > 0
        def handler = body.substring(handlerStart)

        when:
        int showSuccess = handler.indexOf("showSuccessIcon()")
        int scheduleReset = handler.indexOf("delayedExecutor")

        then: "the order keeps the confirmation visible for the intended moment"
        showSuccess > 0
        scheduleReset > 0
        showSuccess < scheduleReset
    }
}
