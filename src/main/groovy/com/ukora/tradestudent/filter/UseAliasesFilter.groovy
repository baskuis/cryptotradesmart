package com.ukora.tradestudent.filter

import com.ukora.tradestudent.services.AliasService

import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class UseAliasesFilter implements Filter {

    AliasService aliasService

    static final DOUBLE_QUOTE = '"'
    static final ALIAS_REQUEST_PARAM = 'alias'
    static final ALIASING_ENABLED_PARAM_VALUE = 'true'

    UseAliasesFilter(AliasService aliasService) {
        this.aliasService = aliasService
    }

    @Override
    void destroy() {}

    @Override
    void doFilter(ServletRequest request, ServletResponse response,
                  FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request
        if (httpRequest.getParameter(ALIAS_REQUEST_PARAM) == ALIASING_ENABLED_PARAM_VALUE) {
            CapturedResponseWrapper capturingResponseWrapper = new CapturedResponseWrapper(
                    (HttpServletResponse) response)
            filterChain.doFilter(request, capturingResponseWrapper)
            String content = capturingResponseWrapper.getCaptureAsString()
            String replacedContent = content
            aliasService.beanToAlias.each {
                replacedContent = replacedContent.replaceAll(
                        DOUBLE_QUOTE + it.key + DOUBLE_QUOTE,
                        DOUBLE_QUOTE + it.value + DOUBLE_QUOTE)
            }
            response.getWriter().write(replacedContent)
        } else {
            filterChain.doFilter(request, response)
        }
    }

    @Override
    void init(FilterConfig filterConfig) throws ServletException {}

    static class CapturedResponseWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream capture
        private ServletOutputStream output
        private PrintWriter writer

        CapturedResponseWrapper(HttpServletResponse response) {
            super(response);
            capture = new ByteArrayOutputStream(response.getBufferSize())
        }

        @Override
        ServletOutputStream getOutputStream() {
            if (writer != null) {
                throw new IllegalStateException(
                        "getWriter() has already been called on this response.");
            }

            if (output == null) {
                output = new ServletOutputStream() {

                    @Override
                    void write(int b) throws IOException {
                        capture.write(b)
                    }

                    @Override
                    void flush() throws IOException {
                        capture.flush()
                    }

                    @Override
                    void close() throws IOException {
                        capture.close()
                    }

                    @Override
                    boolean isReady() {
                        return false
                    }

                    @Override
                    void setWriteListener(WriteListener arg0) {}

                }
            }
            return output
        }

        @Override
        PrintWriter getWriter() throws IOException {
            if (output != null) {
                throw new IllegalStateException(
                        "getOutputStream() has already been called on this response.")
            }
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(capture,
                        getCharacterEncoding()))
            }
            return writer
        }

        @Override
        void flushBuffer() throws IOException {
            super.flushBuffer()
            if (writer != null) {
                writer.flush()
            } else if (output != null) {
                output.flush()
            }
        }

        byte[] getCaptureAsBytes() throws IOException {
            if (writer != null) {
                writer.close()
            } else if (output != null) {
                output.close()
            }
            return capture.toByteArray()
        }

        String getCaptureAsString() throws IOException {
            return new String(getCaptureAsBytes(), getCharacterEncoding())
        }

    }

}
