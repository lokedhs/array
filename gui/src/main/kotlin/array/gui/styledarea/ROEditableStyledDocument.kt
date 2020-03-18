package array.gui.styledarea

import javafx.beans.value.ObservableValue
import org.fxmisc.richtext.model.*
import org.reactfx.EventSource
import org.reactfx.EventStream
import org.reactfx.SuspendableEventStream
import org.reactfx.SuspendableNo
import org.reactfx.collection.LiveList
import org.reactfx.value.Val


class ROEditableStyledDocument : EditableStyledDocument<ParStyle, String, TextStyle> {

    override fun concat(that: StyledDocument<ParStyle, String, TextStyle>?): StyledDocument<ParStyle, String, TextStyle> {
        TODO("Not yet implemented")
    }

    override fun getText(): String {
        TODO("Not yet implemented")
    }

    override fun textProperty(): ObservableValue<String> {
        TODO("Not yet implemented")
    }

    override fun replace(start: Int, end: Int, replacement: StyledDocument<ParStyle, String, TextStyle>?) {
        TODO("Not yet implemented")
    }

    override fun setStyle(from: Int, to: Int, style: TextStyle?) {
        TODO("Not yet implemented")
    }

    override fun setStyle(paragraphIndex: Int, style: TextStyle?) {
        TODO("Not yet implemented")
    }

    override fun setStyle(paragraphIndex: Int, fromCol: Int, toCol: Int, style: TextStyle?) {
        TODO("Not yet implemented")
    }

    override fun getParagraphs(): LiveList<Paragraph<ParStyle, String, TextStyle>> {
        TODO("Not yet implemented")
    }

    private val internalRichChangeList: EventSource<List<RichTextChange<ParStyle, String, TextStyle>>> = EventSource()
    private val richChangeList: SuspendableEventStream<List<RichTextChange<ParStyle, String, TextStyle>>> = internalRichChangeList.pausable()

    override fun multiRichChanges(): EventStream<List<RichTextChange<ParStyle, String, TextStyle>>> {
        return richChangeList
    }

    override fun setStyleSpans(from: Int, styleSpens: StyleSpans<out TextStyle>?) {
        TODO("Not yet implemented")
    }

    override fun setStyleSpans(paragraphIndex: Int, from: Int, styleSpens: StyleSpans<out TextStyle>?) {
        TODO("Not yet implemented")
    }

    override fun getLength(): Int {
        TODO("Not yet implemented")
    }

    override fun replaceMulti(replacements: MutableList<Replacement<ParStyle, String, TextStyle>>?) {
        TODO("Not yet implemented")
    }

    override fun length(): Int {
        TODO("Not yet implemented")
    }

    override fun snapshot(): ReadOnlyStyledDocument<ParStyle, String, TextStyle> {
        TODO("Not yet implemented")
    }

    override fun setParagraphStyle(paragraphIndex: Int, style: ParStyle?) {
        TODO("Not yet implemented")
    }

    override fun isBeingUpdated(): Boolean {
        TODO("Not yet implemented")
    }

    override fun lengthProperty(): Val<Int> {
        TODO("Not yet implemented")
    }

    override fun offsetToPosition(offset: Int, bias: TwoDimensional.Bias?): TwoDimensional.Position {
        TODO("Not yet implemented")
    }

    override fun position(major: Int, minor: Int): TwoDimensional.Position {
        TODO("Not yet implemented")
    }

    override fun subSequence(start: Int, end: Int): StyledDocument<ParStyle, String, TextStyle> {
        TODO("Not yet implemented")
    }

    override fun beingUpdatedProperty(): SuspendableNo {
        TODO("Not yet implemented")
    }

}
