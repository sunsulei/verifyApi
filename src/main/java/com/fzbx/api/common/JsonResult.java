package com.fzbx.api.common;

public class JsonResult {
    private String Code;
    private Boolean success;
    private String msg;
    private Object data;

    private static JsonResult jsonResult = new JsonResult();


    public static JsonResult success(String code, String msg, Object data) {
        jsonResult.setCode(code);
        jsonResult.setMsg(msg);
        jsonResult.setData(data);
        jsonResult.setSuccess(true);
        return jsonResult;
    }

    public static JsonResult error(String code, String msg) {
        jsonResult.setCode(code);
        jsonResult.setMsg(msg);
        jsonResult.setData(null);
        jsonResult.setSuccess(false);
        return jsonResult;
    }


    public String getCode() {
        return Code;
    }

    public void setCode(String code) {
        Code = code;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}
