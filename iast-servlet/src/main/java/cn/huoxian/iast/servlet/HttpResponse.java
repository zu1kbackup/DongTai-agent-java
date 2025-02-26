package cn.huoxian.iast.servlet;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    public static Map<String, Object> getResponse(Object res) {
        HttpServletResponse response = (HttpServletResponse) res;
        Map<String, Object> responseMeta = new HashMap<String, Object>(2);
        responseMeta.put("headers", getHeaders(response));
        responseMeta.put("body", getBody(response));
        return responseMeta;
    }

    /**
     * 获取响应行
     *
     * @param response
     * @return
     */
    private static String getLine(HttpServletResponse response) {
        return "HTTP/1.1" + " " + response.getStatus() + "\n";
    }

    /**
     * 获取HTTP响应头
     *
     * @param response
     * @return
     */
    private static String getHeaders(HttpServletResponse response) {
        StringBuilder header = new StringBuilder();
        Collection<String> headerNames = response.getHeaderNames();
        header.append(getLine(response));
        for (String headerName : headerNames) {
            header.append(headerName).append(":").append(response.getHeader(headerName)).append("\n");
        }
        return header.toString();
    }

    /**
     * 获取响应体
     *
     * @param response
     * @return
     */
    private static String getBody(HttpServletResponse response) {
        String responseStr = "";
        String charSet = "utf-8";
        if (response instanceof ResponseWrapper) {
            try {
                byte[] responseData = ((ResponseWrapper) response).getResponseData();
                String contentType = response.getContentType();
                if (contentType != null && contentType.contains("charset")) {
                    String[] contentTypes = contentType.split(";");
                    for (String contentTypeItem : contentTypes) {
                        if (contentTypeItem.contains("charset")) {
                            charSet = contentTypeItem.trim().replace("charset=", "");
                        }
                    }
                }
                try {
                    responseStr = new String(responseData,0,1000, charSet);
                } catch (UnsupportedEncodingException e) {
                    responseStr = new String(responseData,0,1000);
                }

            } catch (Exception e) {

            }

        }
        return responseStr;
    }
}
