package com.t13max.wxbot.utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 二维码工具类
 *
 * @author t13max
 * @since 16:22 2024/12/7
 */
public class QRUtils {

    public static void print(BufferedImage image, int scale) {
        // 遍历每个像素，将黑色和白色转为字符
        for (int y = 0; y < image.getHeight(); y += scale) {
            for (int x = 0; x < image.getWidth(); x += scale) {
                int pixel = image.getRGB(x, y);
                // 判断是否为黑色
                if (isBlack(pixel)) {
                    System.out.print("███"); // 黑色部分用 █ 表示
                } else {
                    System.out.print("   "); // 白色部分用空格表示
                }
            }
            System.out.println(); // 换行
        }
    }

    public static void print(BufferedImage image) {
        print(image, 15);
    }

    // 判断一个像素是否为黑色
    private static boolean isBlack(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        // 阈值（可以根据需要调整）
        int brightness = (red + green + blue) / 3;
        return brightness < 128; // 明度低于128为黑色
    }

    public static Frame createFrame(BufferedImage image) {
        // 显示图片到窗口中
        JFrame frame = new JFrame("二维码展示");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(250 + 50, 250 + 50); // 设置窗口大小
        frame.setLocationRelativeTo(null); // 居中显示
        // 自定义组件用于绘制图片
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null); // 绘制图片
            }
        };
        frame.add(panel);

        frame.setVisible(true); // 显示窗口
        return frame;
    }

    public static void toPhoto() {
        try {
            // 创建一个简单的BufferedImage对象作为二维码示例
            int size = 200; // 图片的大小
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            // 填充白色背景
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, size, size);

            // 简单绘制黑白方块模拟二维码
            graphics.setColor(Color.BLACK);
            int blockSize = 20; // 方块的大小
            for (int y = 0; y < size; y += blockSize) {
                for (int x = 0; x < size; x += blockSize) {
                    if ((x / blockSize + y / blockSize) % 2 == 0) {
                        graphics.fillRect(x, y, blockSize, blockSize);
                    }
                }
            }
            graphics.dispose();

            // 保存图片到指定路径
            File outputFile = new File("output/qrcode.png");
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs(); // 创建父目录
            }
            ImageIO.write(image, "png", outputFile);

            System.out.println("二维码图片已保存到: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
