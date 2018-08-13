package com.fzbx.api.web;

import com.fzbx.api.common.JsonResult;
import com.fzbx.api.utils.VerifyCodeParser;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VerifyController {

    @RequestMapping(value = "parseCode", method = RequestMethod.POST)
    public JsonResult verityCode(@RequestParam(name = "imageBase64") String image) {
        try {
            String text = VerifyCodeParser.getTextByBase64(image);
            return JsonResult.success("1", "验证码解析成功", text);
        } catch (Exception e) {
            e.printStackTrace();
            return JsonResult.error("0", "验证码解析失败,错误原因:" + e.getMessage());
        }
    }

    @RequestMapping(value = "trainData", method = RequestMethod.GET)
    public JsonResult trainData() {
        try {
            VerifyCodeParser.train();
            return JsonResult.success("1", "重新训练成功且已成功加载新数据", null);
        } catch (Exception e) {
            e.printStackTrace();
            return JsonResult.error("0", "重新训练失败,错误原因:" + e.getMessage());
        }
    }

}
