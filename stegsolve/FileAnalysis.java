/*
 * FileAnalysis.java
 *
 * Created on 27-Apr-2011, 11:14:48
 */

package stegsolve;

import java.awt.*;
import java.io.*;
import java.util.zip.CRC32;

/**
 * File Analysis by examining the file format
 *
 * @author Caesum
 */
public class FileAnalysis extends javax.swing.JFrame {

    /**
     * String containing the file analysis report
     */
    private StringBuilder rep;
    /**
     * The file in bytes
     */
    private byte[] f = null;

    /**
     * Creates new form FileAnalysis
     */
    public FileAnalysis(File ifile) {
        initComponents();
        analyse_file(ifile);
    }

    /**
     * Reads the data from the file and compiles
     * the analysis report
     *
     * @param ifile File to analyse
     */
    private void analyse_file(File ifile) {
        rep = new StringBuilder();
        rep.append("<html><center><b>");
        rep.append("文件格式报告");
        rep.append("</b></center>");
        rep.append("<br>文件：").append(ifile.getName());
        this.f = new byte[(int) ifile.length()];
        try (FileInputStream fis = new FileInputStream(ifile)) {
            int bytesRead = fis.read(f);
            if (bytesRead != f.length) {
                throw new IOException("期望读取 " + f.length + " 字节，但实际只读取了 " + bytesRead + " 字节。");
            }
            rep.append("<br>成功读取 ").append(Integer.toHexString(f.length)).append(" 字节");
            analyse();
        } catch (IOException e) {
            rep.append("<br>读取文件时出错：").append(e.getMessage());
        } catch (Exception e) {
            rep.append("<br>处理文件时出错：").append(e.getMessage());
        }
        rep.append("</html>");
        report.setText(rep.toString());
    }

    /**
     * Checks the header and chooses the correct subtype
     * for analysis
     */
    private void analyse() {
        // analyse f, write report to rep
        if (f.length < 4) {
            rep.append("<br>文件太短？");
            return;
        }
        if (f[0] == 'B' && f[1] == 'M')
            analyse_bmp();
        else if (f[0] == (byte) 137 && f[1] == (byte) 80 && f[2] == 78 && f[3] == 71)
            analyse_png();
        else if (f.length >= 6 && f[0] == 'G' && f[1] == 'I' && f[2] == 'F' && f[3] == '8' && (f[4] == '7' ||
                f[4] == '9') && f[5] == 'a')
            analyse_gif();
        else if (f[0] == (byte) 0xff && f[1] == (byte) 0xd8)
            analyse_jpg();
        else rep.append("<br>文件格式分析代码尚未完成！");
    }

    /**
     * Analysis particular to a jpeg/jpg image
     */
    private void analyse_jpg() {
        int cpos = 0;
        cpos = analyse_jpg_sections(cpos);
        if (cpos < f.length) {
            rep.append("<br>文件末尾的附加字节数 = ").append(f.length - cpos);
            rep.append("<br>转储附加字节:");
            fdump(cpos, f.length);
        }
    }

    /**
     * Analyses one particular jpeg section
     *
     * @param pos Position in the file
     * @return End position in the file
     */
    private int analyse_jpg_sections(int pos) {
        if (f[pos] == (byte) 0xff) {
            switch (f[pos + 1]) {
                case (byte) 0xd8:
                    rep.append("<br>图像的开头");
                    pos += 2;
                    break;
                case (byte) 0xd9:
                    rep.append("<br><br>图像结尾");
                    pos += 2;
                    return pos;
                case (byte) 0xc4:
                    rep.append("<br><br>霍夫曼表");
                    break;
                case (byte) 0xcc:
                    rep.append("<br><br>算术编码条件");
                    break;
                case (byte) 0xda:
                    rep.append("<br><br>扫描开始");
                    break;
                case (byte) 0xdb:
                    rep.append("<br><br>量化表");
                    break;
                case (byte) 0xdc:
                    rep.append("<br><br>定义行数");
                    break;
                case (byte) 0xdd:
                    rep.append("<br><br>定义重启间隔");
                    break;
                case (byte) 0xdf:
                    rep.append("<br><br>扩展参考组件");
                    break;
                case (byte) 0xfe:
                    rep.append("<br><br>注释数据");
                    break;
                case (byte) 0xff:
                    pos++;
                    return analyse_jpg_sections(pos);
                default:
                    if (f[pos + 1] >= (byte) 0xe0 && f[pos + 1] <= (byte) 0xef) {
                        rep.append("<br><br>应用程序数据");
                    } else if (f[pos + 1] >= (byte) 0xd0 && f[pos + 1] <= (byte) 0xd7) {
                        rep.append("<br><br>重启间隔");
                    } else {
                        return pos;
                    }
                    break;
            }
            // 通用处理部分
            if (f[pos + 1] != (byte) 0xff) {
                rep.append("<br>长度: ").append(Integer.toHexString(png_get_word(pos + 2))).append(" (").append(png_get_word(pos + 2)).append(")");
                pos += 2 + png_get_word(pos + 2);
            }
        } else {
            return pos;
        }
        return analyse_jpg_sections(pos);
    }

    /**
     * Analysis particular to a gif image
     */
    private void analyse_gif() {
        if (f.length < 13) {
            rep.append("<br>文件过短?");
            return;
        }
        rep.append("<br>宽度: ").append(get_hword(6)).append(" (").append(get_word(6)).append(")");
        rep.append("<br>高度: ").append(get_hword(8)).append(" (").append(get_word(8)).append(")");
        rep.append("<br>标志: ").append(Integer.toHexString(uf(10)));
        int flags = uf(10);
        int gctsize = 0;
        if ((flags & 0x80) > 0) {
            rep.append(" (全局颜色表)");
            if ((flags & 0x10) > 0) rep.append(" (排序的GCT)");
            gctsize = 1 << (((flags & 0x07) + 1));
            rep.append(" (GCT大小 = ").append(gctsize).append(")");
        }
        rep.append(" (颜色分辨率 = ").append(((flags >> 3) & 0x03) + 1).append(")");
        rep.append("<br>背景色索引: ").append(uf(11));
        rep.append("<br>像素长宽比: ").append(uf(12));
        int cpos = 13;
        if (f.length < cpos + gctsize * 3) {
            rep.append("<br>文件过短?");
            return;
        }
        if (gctsize > 0) {
            rep.append("<br><br>全局颜色表:");
            for (int i = 0; i < gctsize; i++) {
                rep.append("<br>");
                for (int j = 0; j < 3; j++) {
                    rep.append(m2(Integer.toHexString(uf(cpos + i * 3 + j))));
                    rep.append(" ");
                }
                rep.append("   ");
                for (int j = 0; j < 3; j++) {
                    char c = (char) uf(cpos + i * 3 + j);
                    if (c < 32 || c > 0x7f) c = '.';
                    rep.append(c);
                }
            }
            cpos += gctsize * 3;
        }
        cpos = check_gif_blocks(cpos);
        if (cpos < f.length) {
            rep.append("<br>文件末尾额外字节 = ").append(f.length - cpos);
            rep.append("<br>额外字节的数据:");
            fdump(cpos, f.length);
        }
    }

    /**
     * Analysis of a gif block
     *
     * @param pos Position in the file
     * @return Position after this block
     */
    private int check_gif_blocks(int pos) {
        if (f[pos] == 0x2c) {
            if (pos + 10 >= f.length) return pos;
            rep.append("<br><br>图像描述符");
            rep.append("<br>左边距: ").append(get_hword(pos + 1)).append(" (").append(get_word(pos + 1)).append(")");
            rep.append("<br>顶部: ").append(get_hword(pos + 3)).append(" (").append(get_word(pos + 3)).append(")");
            rep.append("<br>宽度: ").append(get_hword(pos + 5)).append(" (").append(get_word(pos + 5)).append(")");
            rep.append("<br>高度: ").append(get_hword(pos + 7)).append(" (").append(get_word(pos + 7)).append(")");
            rep.append("<br>标志: ").append(Integer.toHexString(uf(pos + 9)));
            int flags = uf(pos + 9);
            int lctsize = 0;
            if ((flags & 128) > 0) {
                rep.append(" (局部颜色表)");
                lctsize = 1 << ((flags & 0x07) + 1);
                rep.append(" (LCT大小 = ").append(lctsize);
            }
            if ((flags & 64) > 0) rep.append(" (交错)");
            if ((flags & 32) > 0) rep.append(" (排序)");
            if ((flags & 24) > 0) rep.append(" (**保留标志设置 **)");
            pos = pos + 10;
            if (f.length < pos + lctsize * 3) {
                rep.append("<br>文件过短?");
                return pos;
            }
            if (lctsize > 0) {
                rep.append("<br><br>局部颜色表:");
                for (int i = 0; i < lctsize; i++) {
                    rep.append("<br>");
                    for (int j = 0; j < 3; j++) {
                        rep.append(m2(Integer.toHexString(uf(pos + i * 3 + j))));
                        rep.append(" ");
                    }
                    rep.append("   ");
                    for (int j = 0; j < 3; j++) {
                        char c = (char) uf(pos + i * 3 + j);
                        if (c < 32 || c > 0x7f) c = '.';
                        rep.append(c);
                    }
                }
                pos += lctsize * 3;
            }
            rep.append("<br>LZW大小 = ").append(uf(pos));
            pos++;
            while (uf(pos) > 0) pos += uf(pos) + 1;
            pos++;
        } else if (f[pos] == 0x21) {
            switch (uf(pos + 1)) {
                case 0xf9:
                    if (pos + 8 >= f.length) return pos;
                    rep.append("<br><br>图形控制扩展");
                    rep.append("<br>大小 : ").append(uf(pos + 2)).append(" (必须为4)");
                    rep.append("<br>标志: ").append(Integer.toHexString(uf(pos + 3)));
                    int flags = uf(pos + 3);
                    if ((flags & 224) > 0) rep.append(" (**保留标志设置 **)");
                    if ((flags & 1) > 0) rep.append(" (透明度标志)");
                    rep.append("<br>延迟时间: ").append(get_word(pos + 4));
                    rep.append("<br>透明色索引: ").append(uf(6));
                    rep.append("<br>终止符: ").append(Integer.toHexString(uf(pos + 7)));
                    pos = pos + 8;
                    break;
                case 0xfe:
                    if (pos + 3 >= f.length) return pos;
                    rep.append("<br><br>注释扩展");
                    rep.append("<br>数据转储:");
                    pos += 2;
                    while (uf(pos) > 0) {
                        fdump(pos + 1, pos + uf(pos));
                        pos += uf(pos) + 1;
                    }
                    pos++;
                    break;
                case 0x01:
                    if (pos + 16 >= f.length) return pos;
                    rep.append("<br><br>纯文本扩展");
                    rep.append("<br>大小 : ").append(uf(pos + 2)).append(" (必须为12)");
                    rep.append("<br>左边距: ").append(get_hword(pos + 3)).append(" (").append(get_word(pos + 3)).append(")");
                    rep.append("<br>顶部: ").append(get_hword(pos + 5)).append(" (").append(get_word(pos + 5)).append(")");
                    rep.append("<br>宽度: ").append(get_hword(pos + 7)).append(" (").append(get_word(pos + 7)).append(")");
                    rep.append("<br>高度: ").append(get_hword(pos + 9)).append(" (").append(get_word(pos + 9)).append(")");
                    rep.append("<br>单元格宽度 : ").append(uf(pos + 11));
                    rep.append("<br>单元格高度 : ").append(uf(pos + 12));
                    rep.append("<br>前景色索引 : ").append(uf(pos + 13));
                    rep.append("<br>背景色索引 : ").append(uf(pos + 14));
                    rep.append("<br>数据转储:");
                    pos += 15;
                    while (uf(pos) > 0) {
                        fdump(pos + 1, pos + uf(pos));
                        pos += uf(pos) + 1;
                    }
                    pos++;
                    break;
                case 0xff:
                    if (pos + 14 >= f.length) return pos;
                    rep.append("<br><br>应用程序扩展");
                    rep.append("<br>大小 : ").append(uf(pos + 2)).append(" (必须为11)");
                    rep.append("<br>标识符:");
                    fdump(pos + 3, pos + 10);
                    rep.append("<br>认证代码:");
                    fdump(pos + 11, pos + 13);
                    rep.append("<br>应用程序数据:");
                    pos += 14;
                    while (uf(pos) > 0) {
                        fdump(pos + 1, pos + uf(pos));
                        pos += uf(pos) + 1;
                    }
                    pos++;
                    break;
                default:
                    return pos;
            }
        } else if (f[pos] == 0x3b) {
            rep.append("<br><br>尾部块");
            return pos + 1;
        } else return pos;
        return check_gif_blocks(pos);
    }

    /**
     * Analysis particular to a png file
     */
    private void analyse_png() {
        if (f.length < 8) {
            rep.append("<br>文件过短？");
            return;
        }
        // 校验 PNG 文件头的特定字节
        if (f[4] != 13 || f[5] != 10 || f[6] != 26 || f[7] != 10) {
            rep.append("<br>文件头错误，第5至8字节应为 0d 0a 1a 0a");
        }
        // 检查 PNG 块并返回最后的位置
        int cpos = check_png_chunks(8);
        // 检查文件是否有额外的字节
        if (cpos < f.length) {
            rep.append("<br>文件末尾额外的字节数 = ").append(f.length - cpos);
            rep.append("<br>额外字节的数据:");
            fdump(cpos, f.length);  // 假设 fdump 方法能够输出指定范围内的字节
        }
    }

    /**
     * Analyse png chunks
     *
     * @param start Position in the file
     * @return Position after the chunks
     */
    private int check_png_chunks(int start) {
        if (start + 12 > f.length) {
            rep.append("<br>文件提前结束？");
            return start;
        }
        int length = png_get_dword(start);
        rep.append("<br><br>块: ");
        if ((f[4] & 64) > 0) {
            rep.append("<br>辅助块 - 提供额外信息");
        } else {
            rep.append("<br>关键块 - 必须被识别才能继续显示图像");
        }
        rep.append((f[5] & 64) > 0 ? "<br>私有块，需调查！" : "<br>公共块");
        rep.append((f[6] & 64) > 0 ? "<br>块设置了保留标志，**需调查**！" : "");
        rep.append((f[7] & 64) > 0 ? "<br>安全复制，块可以传播到其他文件" : "<br>不安全复制，除非软件知道");

        // 打印块类型和块数据
        fdump(start + 4, start + 7);
        rep.append("<br>数据长度 = ").append(length).append(" 字节");
        rep.append("<br>CRC = ").append(png_get_hdword(start + 8 + length));

        // 校验CRC
        CRC32 cc = new CRC32();
        cc.reset();
        cc.update(f, start + 4, 4 + length);
        if ((int) cc.getValue() != png_get_dword(start + 8 + length)) {
            rep.append("<br>计算的CRC = ").append(Integer.toHexString((int) cc.getValue()));
        }

        if (start + 12 + length > f.length) {
            rep.append("<br>文件中没有足够的空间存储数据？");
            return start;
        }

        int typ = png_get_dword(start + 4);
        switch (typ) {
            case 0x49454E44: // IEND
                return start + 12;
            case 0x49484452: // IHDR
                rep.append("<br>宽度: ").append(png_get_hdword(start + 8)).append(" (").append(png_get_dword(start + 8)).append(")")
                        .append("<br>高度: ").append(png_get_hdword(start + 12)).append(" (").append(png_get_dword(start + 12)).append(")")
                        .append("<br>位深: ").append(f[start + 16])
                        .append("<br>颜色类型: ").append(colorTypeDescription(f[start + 17]))
                        .append("<br>压缩方法: ").append(f[start + 18] == 0 ? " (deflate)" : " (未知)")
                        .append("<br>过滤方法: ").append(f[start + 19] == 0 ? " (自适应)" : " (未知)")
                        .append("<br>隔行扫描方法: ").append(interlaceDescription(f[start + 20]));
                break;
            case 0x504C5445: // PLTE
                rep.append("<br>调色板包含 ").append(length / 3).append(" RGB条目，注意：如果颜色类型是2或6，这可能隐藏了一些信息。");
                break;
            case 0x49444154: // IDAT
                rep.append("<br>图像数据，已压缩");
                break;
            default:
                rep.append("<br>未知块类型");
                break;
        }
        // 打印剩余块的数据
        fdump(start + 8, start + 8 + length - 1);
        return check_png_chunks(start + 12 + length);
    }

    private String colorTypeDescription(byte type) {
        switch (type) {
            case 0:
                return "灰度";
            case 2:
                return "RGB三元组";
            case 3:
                return "调色板索引";
            case 4:
                return "灰度+Alpha";
            case 6:
                return "RGB+Alpha";
            default:
                return "未知";
        }
    }

    private String interlaceDescription(byte method) {
        switch (method) {
            case 0:
                return "无";
            case 1:
                return "Adam7";
            default:
                return "未知";
        }
    }

    /**
     * Analysis particular to a bmp file
     */
    private void analyse_bmp() {
        int fsz = get_dword(2);
        int offbits = get_dword(10);
        if (f.length < 54) {
            rep.append("<br>文件太短，不足以包含头部信息");
            return;
        }
        rep.append("<br>文件头信息：");
        rep.append("<br>文件大小：").append(Integer.toHexString(fsz));
        rep.append("<br>保留字1：").append(get_hword(6));
        rep.append("<br>保留字2：").append(get_hword(8));
        if (f.length > fsz) {
            rep.append("<br>文件末尾额外字节 = ").append(f.length - fsz);
            rep.append("<br>额外字节内容：");
            fdump(fsz, f.length - 1);
            rep.append("<br>注意：额外字节可能表明头部中的高度设置不正确，尝试增加高度以查看是否隐藏有图像数据");
        }
        rep.append("<br>位图数据开始于：").append(Integer.toHexString(offbits));
        rep.append("<br>信息头：");
        int bisize = get_dword(14);
        rep.append("<br>信息头大小：").append(Integer.toHexString(bisize));
        rep.append("<br>宽度：").append(get_hdword(18)).append(" (").append(get_dword(18)).append(")");
        rep.append("<br>高度：").append(get_hdword(22)).append(" (").append(get_dword(22)).append(")");
        rep.append("<br>平面数：").append(get_hword(26)).append(" (必须为1)");
        rep.append("<br>每像素位数：").append(get_hword(28)).append(" 位");
        int compress = get_dword(30);
        rep.append("<br>压缩方式：").append(get_hdword(30));
        switch (compress) {
            case 0x00:
            case 0x32424752:
                rep.append(" (无压缩)");
                break;
            case 0x01:
            case 0x38454c52:
                rep.append(" (8位RGB游程编码)");
                break;
            case 0x02:
            case 0x34454c52:
                rep.append(" (4位RGB游程编码)");
                break;
            case 0x03:
                rep.append(" (打包RGB)");
                break;
            case 0x32776173:
                rep.append(" (原始RGB)");
                break;
            case 0x41424752:
                rep.append(" (带Alpha的原始RGB)");
                break;
            case 0x54424752:
                rep.append(" (带透明度的原始RGB)");
                break;
        }
        rep.append("<br>图像大小：").append(get_hdword(34)).append(" (如果位图未压缩则可能为0)");
        rep.append("<br>水平分辨率：").append(get_hdword(38)).append(" (目标设备的每米像素数)");
        rep.append("<br>垂直分辨率：").append(get_hdword(42)).append(" (目标设备的每米像素数)");
        rep.append("<br>颜色使用数：").append(get_hdword(46)).append(" (使用的颜色索引数，0=最大)");
        rep.append("<br>重要颜色数：").append(get_hdword(50)).append(" (0表示所有颜色均重要)");
        int ctstart = 14 + bisize;
        rep.append("<br>颜色表计算起始位置：").append(Integer.toHexString(ctstart)).append(" (通常为0x36，如果不是则可能隐藏了数据)");
        if (ctstart > 0x36) {
            rep.append("<br>头部与颜色表开始之间的间隙数据：");
            fdump(0x36, ctstart - 1);
        }
        if (get_word(28) <= 8) {
            rep.append("<br>颜色索引表（注意：每个索引的第四个条目应为零，顺序为b,g,r,a）：");
            int ncols = get_dword(46);
            if (ncols == 0) {
                ncols = 1 << get_word(28);
            }
            for (int i = 0; i < ncols; i++) {
                rep.append("<br>");
                for (int j = 0; j < 4; j++) {
                    rep.append(m2(Integer.toHexString(uf(ctstart + i * 4 + j)))).append(" ");
                }
                rep.append("   ");
                for (int j = 0; j < 4; j++) {
                    char c = (char) uf(ctstart + i * 4 + j);
                    if (c < 32 || c > 0x7f) c = '.';
                    rep.append(c);
                }
            }
            ctstart += ncols * 4;
        }
        if (ctstart != offbits) {
            rep.append("<br>颜色表结束于 ").append(Integer.toHexString(ctstart)).append(" 但数据位开始于 ").append(Integer.toHexString(offbits));
        }
        if (ctstart < offbits) {
            rep.append("<br>颜色表与图像数据之间的间隙数据：");
            fdump(ctstart, offbits - 1);
        }
        if (compress != 0x00 && compress != 0x32424752) {
            rep.append("<br>仅在无压缩情况下进一步检查");
            return;
        }
        rep.append("<br>行填充字节数据：");
        int bitsPerRow = get_dword(18) * get_word(28);
        int bytesPerRow = (bitsPerRow + 7) / 8;
        int fgap = (bytesPerRow + 3) / 4 * 4 - bytesPerRow;
        ctstart = offbits;
        for (int i = 0; i < get_dword(22); i++) {
            ctstart += bytesPerRow;
            int ctend = ctstart + fgap;
            if (ctend != ctstart) {
                fdump(ctstart, ctend - 1);
            }
            ctstart = ctend;
        }
    }

    /**
     * Dumps a section of file
     *
     * @param from Dump from this offset
     * @param to   Dump to this offset
     */
    private void fdump(int from, int to) {
        // 检查起始位置是否超出数组范围
        if (from >= f.length) {
            return;
        }

        // 输出十六进制表示
        rep.append("<br>十六进制:");
        for (int i = from; i <= to && i < f.length; i += 16) {
            rep.append("<br>");
            for (int j = 0; j < 16 && i + j < f.length && i + j <= to; j++) {
                rep.append(m2(Integer.toHexString(((int) f[i + j]) & 0xff)));
                if (j == 7) {
                    rep.append(' '); // 在中间添加空格以提高可读性
                }
            }
            rep.append("  "); // 每行末尾额外的空格，可考虑删除以简化输出
        }

        // 输出ASCII表示
        rep.append("<br>ASCII:");
        for (int i = from; i <= to && i < f.length; i += 16) {
            rep.append("<br>");
            for (int j = 0; j < 16 && i + j < f.length && i + j <= to; j++) {
                char c = (char) f[i + j];
                if (c == '<') {
                    rep.append("&lt;");
                } else if (c == '>') {
                    rep.append("&gt;");
                } else if (c == '&') {
                    rep.append("&amp;");
                } else if (c >= 32 && c <= 128) {
                    rep.append(c);
                } else {
                    rep.append('.');
                }
                if (j == 7) {
                    rep.append(' '); // 在中间添加空格以提高可读性
                }
            }
        }
    }

    /**
     * Ensures a hex string is 2 bytes long, adding a leading zero if it is not
     *
     * @param hx hex string
     */
    private String m2(String hx) {
        if (hx.length() < 2)
            return "0" + hx;
        return hx;
    }

    /**
     * Returns hex of dword at given file offset (lsb...msb)
     *
     * @param offs File offset
     * @return Hex string
     */
    private String get_hdword(int offs) {
        return Integer.toHexString(get_dword(offs));
    }

    /**
     * Returns hex of dword at given file offset (msb...lsb)
     *
     * @param offs File offset
     * @return Hex string
     */
    private String png_get_hdword(int offs) {
        return Integer.toHexString(png_get_dword(offs));
    }

    /**
     * Returns hex of word at given file offset (lsb...msb)
     *
     * @param offs File offset
     * @return Hex string
     */
    private String get_hword(int offs) {
        return Integer.toHexString(get_word(offs));
    }

    /**
     * Returns byte at given file offset
     *
     * @param offs File offset
     * @return byte at file offset, as int
     */
    private int uf(int offs) {
        if (offs >= f.length)
            return 0;
        int r = f[offs];
        return r & 0xff;
    }

    /**
     * Returns dword at given file offset (lsb...msb)
     *
     * @param offs File offset
     * @return dword at file offset, as int
     */
    private int get_dword(int offs) {
        if (offs + 3 >= f.length)
            return 0;
        return uf(offs) + (uf(offs + 1) << 8) + (uf(offs + 2) << 16) + (uf(offs + 3) << 24);
    }

    /**
     * Returns dword at given file offset (msb...lsb)
     *
     * @param offs File offset
     * @return dword at file offset, as int
     */
    private int png_get_dword(int offs) {
        if (offs + 3 >= f.length)
            return 0;
        return uf(offs + 3) + (uf(offs + 2) << 8) + (uf(offs + 1) << 16) + (uf(offs) << 24);
    }

    /**
     * Returns word at given file offset (lsb...msb)
     *
     * @param offs File offset
     * @return word at file offset, as int
     */
    private int get_word(int offs) {
        if (offs + 1 >= f.length)
            return 0;
        return uf(offs) + (uf(offs + 1) << 8);
    }

    /**
     * Returns word at given file offset (msb...lsb)
     *
     * @param offs File offset
     * @return word at file offset, as int
     */
    private int png_get_word(int offs) {
        if (offs + 1 >= f.length)
            return 0;
        return uf(offs + 1) + (uf(offs) << 8);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        report = new javax.swing.JEditorPane();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        // Variables declaration - do not modify//GEN-BEGIN:variables
        javax.swing.JButton OKButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("文件格式分析");

        jPanel1.setMinimumSize(new java.awt.Dimension(400, 300));
        jPanel1.setPreferredSize(new java.awt.Dimension(400, 350));

        jScrollPane1.setMinimumSize(new java.awt.Dimension(400, 260));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(400, 260));

        report.setContentType("text/html");
        report.setFont(new java.awt.Font("微软雅黑", Font.PLAIN, 14)); // NOI18N
        jScrollPane1.setViewportView(report);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 815, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel2.setMinimumSize(new java.awt.Dimension(400, 35));
        jPanel2.setPreferredSize(new java.awt.Dimension(400, 35));

        OKButton.setText("确认");
        OKButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        OKButton.addActionListener(this::OKButtonActionPerformed);
        jPanel2.add(OKButton);

        getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Close the form
     *
     * @param evt Event
     */
    private void OKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OKButtonActionPerformed
        dispose();
    }//GEN-LAST:event_OKButtonActionPerformed


    private javax.swing.JEditorPane report;
    // End of variables declaration//GEN-END:variables

}
