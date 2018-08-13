package com.fzbx.api.utils;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerifyCodeParser {

    private static Map<BufferedImage, String> trainMap = new HashMap<>();
    public static Map<String, String> trainMapKeyIsFileMd5 = new HashMap<>();
    private static String verifyPath = "/opt/app/jboss_test/verify/";
    private static File trainDir = new File(VerifyCodeParser.verifyPath + "/train");
    private static File tempDir = new File(VerifyCodeParser.verifyPath + "/temp");

    @Value("${verify.path}")
    public void setTrainDataPath(String verifyPath) {
        VerifyCodeParser.verifyPath = verifyPath;
        trainDir = new File(VerifyCodeParser.verifyPath + "/train");
        tempDir = new File(VerifyCodeParser.verifyPath + "/temp");
        System.out.println("训练数据文件路径 : " + VerifyCodeParser.verifyPath);
        try {
            loadTrainData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 训练并且重新加载
     *
     * @throws Exception
     */
    public static void train() throws Exception {
        trainData();
        loadTrainData();
        System.out.println("训练并重新加载成功.图片总数量: " + trainMap.size());
    }

    /**
     * 当前像素是否是黑色块
     *
     * @param colorInt:当前像素点color值
     * @return
     */
    private static boolean isBlack(int colorInt) {
        Color color = new Color(colorInt);
        return color.getRed() + color.getGreen() + color.getBlue() <= 100;
    }

    /**
     * 获得二值化图像 最大类间方差法
     *
     * @param gray
     * @param width
     * @param height
     * @return
     */
    private static int getOstu(int[][] gray, int width, int height) {
        int grayLevel = 256;
        int[] pixelNum = new int[grayLevel];
        // 计算所有色阶的直方图
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = gray[x][y];
                pixelNum[color]++;
            }
        }
        double sum = 0;
        int total = 0;
        for (int i = 0; i < grayLevel; i++) {
            sum += i * pixelNum[i]; // x*f(x)质量矩，也就是每个灰度的值乘以其点数（归一化后为概率），sum为其总和
            total += pixelNum[i]; // n为图象总的点数，归一化后就是累积概率
        }
        double sumB = 0;// 前景色质量矩总和
        int threshold = 0;
        double wF = 0;// 前景色权重
        double wB = 0;// 背景色权重

        double maxFreq = -1.0;// 最大类间方差

        for (int i = 0; i < grayLevel; i++) {
            wB += pixelNum[i]; // wB为在当前阈值背景图象的点数
            if (wB == 0) { // 没有分出前景后景
                continue;
            }

            wF = total - wB; // wB为在当前阈值前景图象的点数
            if (wF == 0) {// 全是前景图像，则可以直接break
                break;
            }

            sumB += (double) (i * pixelNum[i]);
            double meanB = sumB / wB;
            double meanF = (sum - sumB) / wF;
            // freq为类间方差
            double freq = wF * wB * (meanB - meanF) * (meanB - meanF);
            if (freq > maxFreq) {
                maxFreq = freq;
                threshold = i;
            }
        }

        return threshold;
    }

    /**
     * 图片预处理 灰度化、二值化、去噪
     *
     * @param img
     * @return
     * @throws Exception
     */
    public static BufferedImage removeBackgroud(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        double Wr = 0.299;
        double Wg = 0.587;
        double Wb = 0.114;

        int[][] gray = new int[width][height];

        // 灰度化
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = new Color(img.getRGB(x, y));
                int rgb = (int) ((color.getRed() * Wr + color.getGreen() * Wg + color.getBlue() * Wb) / 3);
                gray[x][y] = rgb;
            }
        }
        int ostu = getOstu(gray, width, height);

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (gray[x][y] > ostu) {
                    img.setRGB(x, y, Color.white.getRGB());
                } else {
                    img.setRGB(x, y, Color.black.getRGB());
                }

            }
        }

        // 去噪
        X:
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (isBlack(img.getRGB(x, y))) {
                    if (isAlone(img, x, y, width, height)) {
                        img.setRGB(x, y, Color.WHITE.getRGB());
                        //去掉一个点后重新根据x扫描.
                        if (x > 5) {
                            x = x - 1;
                            continue X;
                        }
                        y = 0;
                    }
                }
            }
        }
        return img;
    }

    /**
     * 是否单个噪点 目前判断当前像素点的上下左右4个点是否有黑点，可判断8个方位点
     */
    private static boolean isAlone(BufferedImage img, int x, int y, int width, int height) {

        if (x == 0 || width - x <= 1 || y == 0 || height - y <= 1) {
            return true;
        }
        int top = y + 1;
        int bottom = y - 1;
        int left = x - 1;
        int right = x + 1;
        try {
            boolean a1 = isBlack(img.getRGB(left, top));
            boolean a2 = isBlack(img.getRGB(left, y));
            boolean a3 = isBlack(img.getRGB(left, bottom));
            boolean a4 = isBlack(img.getRGB(x, top));
            boolean a5 = isBlack(img.getRGB(x, bottom));
            boolean a6 = isBlack(img.getRGB(right, top));
            boolean a7 = isBlack(img.getRGB(right, y));
            boolean a8 = isBlack(img.getRGB(right, bottom));

            List<Boolean> list = new ArrayList<>();
            if (a1) list.add(true);
            if (a2) list.add(true);
            if (a3) list.add(true);
            if (a4) list.add(true);
            if (a5) list.add(true);
            if (a6) list.add(true);
            if (a7) list.add(true);
            if (a8) list.add(true);

            int count = list.size();

            if (count == 2) {
                return a2 && (a1 || a3)
                        || a4 && (a1 || a6)
                        || a5 && (a3 || a8)
                        || a7 && (a6 || a8);
            }

            if (count <= 1) {
                return true;
            }

            if (count == 3) {
                return (a1 && a2 && a3) ||
                        (a1 && a4 && a6) ||
                        (a3 && a5 && a8) ||
                        (a6 && a7 && a8);
            }
        } catch (Exception e) {
            return false;
        }
        return false;

    }

    /**
     * 移除空白像素
     */
    private static BufferedImage removeBlank(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int start = 0;
        int end = 0;
        Label1:
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                if (isBlack(img.getRGB(x, y))) {
                    start = y;
                    break Label1;
                }
            }
        }
        Label2:
        for (int y = height - 1; y >= 0; --y) {
            for (int x = 0; x < width; ++x) {
                if (isBlack(img.getRGB(x, y))) {
                    end = y;
                    break Label2;
                }
            }
        }
        return img.getSubimage(0, start, width, end - start + 1);
    }

    /**
     * 分割图片
     * 把宽对应的每一个像素判断是不是黑色.统计每个宽对应列的所有黑色的点的个数.
     * <p>
     * 截取时以第一个竖列有黑色开始 宽循环
     */
    private static List<BufferedImage> splitImage(BufferedImage img) {
        List<BufferedImage> subImgs = new ArrayList<>();
        int width = img.getWidth();
        int height = img.getHeight();
        List<Integer> weightlist = new ArrayList<>();
        for (int x = 0; x < width; ++x) {
            int count = 0;
            for (int y = 0; y < height; ++y) {
                if (isBlack(img.getRGB(x, y))) {
                    count++;
                }
            }
            weightlist.add(count);
        }
        for (int i = 0; i < weightlist.size(); i++) {
            if (weightlist.get(i) < 1) {
                continue;
            }
            int length = 0;
            while (i < weightlist.size() && weightlist.get(i) > 0) {
                i++;
                length++;
            }
            if (length > 2) {
                subImgs.add(removeBlank(img.getSubimage(i - length, 0, length, height)));
            }
        }
        return subImgs;
    }

    /**
     * 加载训练图片
     */
    private static void loadTrainData() throws Exception {
        trainMapKeyIsFileMd5.clear();
        trainMap.clear();
        boolean b = trainDir.canRead();
        File[] files = trainDir.listFiles();
        System.out.println("train文件夹是否可读 : " + b);
        System.out.println("train文件夹总图片文件数量 : " + files.length);
        if (files.length < 1) {
            return;
        }

        for (File file : files) {
            if (!file.getName().endsWith(".png")) {
                continue;
            }
            String md5 = DigestUtil.md5Hex(file);
            if (trainMapKeyIsFileMd5.containsKey(md5)) {
                BufferedImage old = ImageIO.read(file);
                BufferedImage read = ImageIO.read(file);
                FileUtil.del(file);
                continue;
            }
            trainMapKeyIsFileMd5.put(md5, file.getName().charAt(0) + "");

            String name = file.getName();
            BufferedImage read = ImageIO.read(file);
            if (trainMap.containsKey(read)) {
                System.out.println("训练结果发现重复文件,文件名: " + name + " , 对应的image: " + JSONUtil.toJsonPrettyStr(read));
                continue;
            }
            trainMap.put(read, name.charAt(0) + "");
        }
    }

    /**
     * 匹配单个图片信息
     */
    private static String getSingleCharOcr(BufferedImage img) {
        try {
            File temp = File.createTempFile("img", ".png");
            ImageIO.write(img, "png", temp);
            String md5 = DigestUtil.md5Hex(temp);
            if (trainMapKeyIsFileMd5.containsKey(md5)) {
                return trainMapKeyIsFileMd5.get(md5);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String result = "#";
        int width = img.getWidth();
        int height = img.getHeight();
        int min = width * height;
        Label1:
        for (BufferedImage bi : trainMap.keySet()) {
            int count = 0;
            if (Math.abs(bi.getWidth() - width) > 2)
                continue;
            int widthmin = width < bi.getWidth() ? width : bi.getWidth();
            int heightmin = height < bi.getHeight() ? height : bi.getHeight();
            for (int x = 0; x < widthmin; ++x) {
                for (int y = 0; y < heightmin; ++y) {
                    if (isBlack(img.getRGB(x, y)) != isBlack(bi.getRGB(x, y))) {
                        count++;
                        if (count > min) {
                            continue Label1;
                        }
                    }
                }
            }

            if (count < min) {
                min = count;
                result = trainMap.get(bi);
            }
            if (count == 0 && min == 0) {
                break;
            }
//            if (count < min && count != 0) {
//                min = count;
//                result = trainMap.get(bi);
//            }
//            if (count == 0 && min == 1 && width == bi.getWidth() && height == bi.getHeight()) {
//                result = trainMap.get(bi);
//                break;
//            }
        }
        return result;
    }

    /**
     * @param read
     */
    private static String getTextByBufferedImage(BufferedImage read) {

        // 二值化、去噪
        removeBackgroud(read);

        // 分割图片
        List<BufferedImage> listImg = splitImage(read);

        StringBuilder result = new StringBuilder();
        // 循环匹配单个图片
        for (BufferedImage bi : listImg) {
            result.append(getSingleCharOcr(bi));
        }

        return result.toString();
    }

    /**
     * 根据文件路径得到验证码
     *
     * @param fileStr 文件路劲+文件名
     */
    public static String getTextByFilePath(String fileStr) throws Exception {

        File file = new File(fileStr);
        BufferedImage read = ImageIO.read(file);

        return getTextByBufferedImage(read);
    }

    /**
     * 根据图片Url地址得到验证码
     */
    public static String getTextByImageUrl(String urlStr) throws Exception {

        URL url = new URL(urlStr);
        BufferedImage read = ImageIO.read(url);

        return getTextByBufferedImage(read);
    }

    /**
     * 通过Base64编码得到验证码
     */
    public static String getTextByBase64(String base64Text) throws Exception {
        byte[] decode = Base64.decodeBase64(base64Text);

        BufferedImage read = ImageIO.read(new ByteArrayInputStream(decode));

        return getTextByBufferedImage(read);
    }

    /**
     * 数据训练
     */
    private static void trainData() throws Exception {
        boolean b = tempDir.canRead();
        System.out.println("temp文件夹是否可读 : " + b);
        FileUtil.clean(trainDir);
        File[] files = tempDir.listFiles();
        if (files == null || files.length < 1) {
            return;
        }
        for (File file : files) {
            if (!file.getName().endsWith(".jpg")) {
                continue;
            }
            // 图片预处理 二值化、去噪
            BufferedImage img = removeBackgroud(ImageIO.read(file));
            // 图片分割
            List<BufferedImage> listImg = splitImage(img);
            if (listImg.size() == 4) {
                for (int j = 0; j < listImg.size(); ++j) {
                    char code = file.getName().charAt(j);
                    File single = new File(trainDir.getPath() + "/" + code + "-" + (RandomUtil.simpleUUID()) + ".png");
                    ImageIO.write(listImg.get(j), "png", single);
                }
            }
        }
    }

    public static void main(String[] args) {

    }

}