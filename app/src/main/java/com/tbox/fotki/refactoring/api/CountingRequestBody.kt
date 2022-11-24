package com.tbox.fotki.refactoring.api

import okhttp3.RequestBody
import okio.*
import java.lang.ref.WeakReference

interface OnByteWrittenListener {
    fun onByteWritten(byteWritten: Long, contentLength: Long)
}

internal class CountingRequestBody(private val delegate: RequestBody, listener: OnByteWrittenListener) : RequestBody() {

    private val weakListener = listener.weak()

    override fun contentType() = delegate.contentType()

    override fun writeTo(sink: BufferedSink) {
        val bufferedSink = CountingSink(
            sink,
            delegate.contentLength(),
            weakListener
        )
            .buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    internal class CountingSink(
        delegate: Sink,
        private val contentLength: Long,
        private val weakListener: WeakReference<OnByteWrittenListener>
    ) : ForwardingSink(delegate) {
        private var bytesWritten: Long = 0L
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            weakListener.get()?.onByteWritten(bytesWritten, contentLength)
        }
    }
}

fun RequestBody.counting(listener: OnByteWrittenListener): RequestBody =
    CountingRequestBody(this, listener)
