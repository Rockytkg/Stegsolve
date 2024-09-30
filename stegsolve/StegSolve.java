/*
 * StegSolve.java
 *
 * Created on 18-Apr-2011, 08:48:02
 */

package stegsolve;

import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.datatransfer.DataFlavor;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.dnd.*;
import java.util.ArrayList;
import java.util.List;

// todo - sort out dimensions in linux
// todo - width/height explorer

/**
 * StegSolve
 *
 * @author Caesum
 */
public class StegSolve extends JFrame {
    static StegSolve that;
    /**
     * label that shows the number of the frame currently being shown
     */
    private JLabel nowShowing;
    private ZoomSlider zoomSlider;
    /**
     * Panel with image on it
     */
    private DPanel dp;
    /**
     * Scroll bars for panel with image
     */
    private JScrollPane scrollPane;

    /**
     * The image file
     */
    private File sfile = null;
    /**
     * The image
     */
    private BufferedImage bi = null;
    /**
     * The transformation being viewed
     */
    private Transform transform = null;

    // <editor-fold defaultstate="collapsed" desc="Initcomponents()">
    private void initComponents() {
        new DropTarget(this, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Object transferData = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (transferData instanceof List<?>) {
                        try {
                            List<File> droppedFiles = getFiles((List<?>) transferData);
                            if (!droppedFiles.isEmpty()) {
                                loadImage(droppedFiles.get(0));
                            }
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "拖放操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }

            private List<File> getFiles(List<?> transferData) throws Exception {
                List<File> droppedFiles = new ArrayList<>();
                for (Object obj : transferData) {
                    if (obj instanceof File) {
                        File file = (File) obj;
                        String filename = file.getName().toLowerCase();
                        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")
                                || filename.endsWith(".gif") || filename.endsWith(".bmp")) {
                            droppedFiles.add(file);
                        } else {
                            throw new Exception("非图片文件：" + filename);
                        }
                    }
                }
                return droppedFiles;
            }
        });

        FlatIntelliJLaf.install();
        JMenuBar menuBar = new JMenuBar();
        JMenu menuFile = new JMenu();
        JMenuItem fileOpen = new JMenuItem();
        JMenuItem fileSave = new JMenuItem();
        JMenuItem fileExit = new JMenuItem();
        JMenu menuAnalyse = new JMenu();
        JMenuItem analyseFormat = new JMenuItem();
        JMenuItem analyseExtract = new JMenuItem();
        JMenuItem stereoSolve = new JMenuItem();
        JMenuItem frameBrowse = new JMenuItem();
        JMenuItem imageCombine = new JMenuItem();
        JMenu menuHelp = new JMenu();
        JMenuItem about = new JMenuItem();
        nowShowing = new JLabel();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        menuFile.setText("文件");

        fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0));
        fileOpen.setText("打开");
        fileOpen.addActionListener(this::fileOpenActionPerformed);
        menuFile.add(fileOpen);

        fileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
        fileSave.setText("另存为");
        fileSave.addActionListener(this::fileSaveActionPerformed);
        menuFile.add(fileSave);

        fileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0));
        fileExit.setText("退出");
        fileExit.addActionListener(this::fileExitActionPerformed);
        menuFile.add(fileExit);

        menuBar.add(menuFile);

        menuAnalyse.setText("分析");

        analyseFormat.setText("文件格式");
        analyseFormat.addActionListener(this::analyseFormatActionPerformed);
        menuAnalyse.add(analyseFormat);

        analyseExtract.setText("数据提取");
        analyseExtract.addActionListener(this::analyseExtractActionPerformed);
        menuAnalyse.add(analyseExtract);

        stereoSolve.setText("立体视图");
        stereoSolve.addActionListener(this::stereoSolveActionPerformed);
        menuAnalyse.add(stereoSolve);

        frameBrowse.setText("帧浏览器");
        frameBrowse.addActionListener(this::frameBrowseActionPerformed);
        menuAnalyse.add(frameBrowse);

        imageCombine.setText("图像合成器");
        imageCombine.addActionListener(this::imageCombineActionPerformed);
        menuAnalyse.add(imageCombine);

        menuBar.add(menuAnalyse);

        menuHelp.setText("帮助");

        about.setText("关于");
        about.addActionListener(this::aboutActionPerformed);
        menuHelp.add(about);

        menuBar.add(menuHelp);

        setJMenuBar(menuBar);

        setLayout(new BorderLayout());

        JPanel textZoom = new JPanel();
        textZoom.setLayout(new BorderLayout());

        textZoom.add(nowShowing, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        JButton backwardButton = new JButton("<");
        backwardButton.addActionListener(this::backwardButtonActionPerformed);
        JButton forwardButton = new JButton(">");
        forwardButton.addActionListener(this::forwardButtonActionPerformed);
        buttonPanel.add(backwardButton);
        buttonPanel.add(forwardButton);

        add(buttonPanel, BorderLayout.SOUTH);

        zoomSlider = new ZoomSlider(10, 1000, 100);

        zoomSlider.addChangeListener(v -> {
            dp.apply(v);
            dp.revalidate();
        });

        textZoom.add(zoomSlider, BorderLayout.SOUTH);

        add(textZoom, BorderLayout.NORTH);

        dp = new DPanel();
        scrollPane = new JScrollPane(dp);

        //Horizontal scrolling
        JFrame frame = this;
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub
                if (e.isShiftDown()) {
                    frame.addMouseWheelListener(arg01 -> {
                        // TODO Auto-generated method stub
                        scrollPane.getHorizontalScrollBar().setValue(scrollPane.getHorizontalScrollBar().getValue() + arg01.getWheelRotation());
                    });
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!e.isShiftDown()) {
                    frame.removeMouseWheelListener(frame.getMouseWheelListeners()[0]);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });

        add(scrollPane, BorderLayout.CENTER);

        backwardButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "back");
        backwardButton.getActionMap().put("back", backButtonPress);
        forwardButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "forward");
        forwardButton.getActionMap().put("forward", forwardButtonPress);

        this.setTitle("StegSolve");
        this.setMaximumSize(getToolkit().getScreenSize());

        pack();

        this.setSize(800, 600);
    }

    /**
     * Close the form on file exit
     *
     * @param evt Event
     */
    private void fileExitActionPerformed(ActionEvent evt) {
        dispose();
    }

    /**
     * This is used to map the left arrow key to the back button
     */
    private final Action backButtonPress = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            backwardButtonActionPerformed(e);
        }
    };

    /**
     * Move back by one image
     *
     * @param evt Event
     */
    private void backwardButtonActionPerformed(ActionEvent evt) {
        if (transform == null) return;
        transform.back();
        updateImage();
    }

    /**
     * This is used to map the right arrow key to the forward button
     */
    private final Action forwardButtonPress = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            forwardButtonActionPerformed(e);
        }
    };

    /**
     * Move forward by one image
     *
     * @param evt Event
     */
    private void forwardButtonActionPerformed(ActionEvent evt) {
        if (bi == null) return;
        transform.forward();
        updateImage();
    }

    /**
     * Show the help/about frame
     *
     * @param evt Event
     */
    private void aboutActionPerformed(ActionEvent evt) {
        new AboutFrame().setVisible(true);
    }

    /**
     * Open the file format analyser
     *
     * @param evt Event
     */
    private void analyseFormatActionPerformed(ActionEvent evt) {
        new FileAnalysis(sfile).setVisible(true);
    }

    /**
     * Open the stereogram solver
     *
     * @param evt Event
     */
    private void stereoSolveActionPerformed(ActionEvent evt) {
        new Stereo(bi).setVisible(true);
    }

    /**
     * Open the frame browser
     *
     * @param evt Event
     */
    private void frameBrowseActionPerformed(ActionEvent evt) {
        new FrameBrowser(bi, sfile).setVisible(true);
    }

    /**
     * Open the image combiner
     *
     * @param evt Event
     */
    private void imageCombineActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "jpg", "jpeg", "gif", "bmp", "png");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("选择要合并的图像");
        int rVal = fileChooser.showOpenDialog(this);
        System.setProperty("user.dir", fileChooser.getCurrentDirectory().getAbsolutePath());
        if (rVal == JFileChooser.APPROVE_OPTION) {
            sfile = fileChooser.getSelectedFile();
            try {
                BufferedImage bi2;
                bi2 = ImageIO.read(sfile);
                new Combiner(bi, bi2).setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "加载文件失败: " + e);
            }
        }
    }

    /**
     * Open the data extractor
     *
     * @param evt Event
     */
    private void analyseExtractActionPerformed(ActionEvent evt) {
        new Extract(bi).setVisible(true);
    }

    /**
     * Save the current transformed image
     *
     * @param evt Event
     */
    private void fileSaveActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        fileChooser.setSelectedFile(new File("solved.bmp"));
        int rVal = fileChooser.showSaveDialog(this);
        System.setProperty("user.dir", fileChooser.getCurrentDirectory().getAbsolutePath());
        if (rVal == JFileChooser.APPROVE_OPTION) {
            sfile = fileChooser.getSelectedFile();
            try {
                bi = transform.getImage();
                int rns = sfile.getName().lastIndexOf(".") + 1;
                if (rns == 0)
                    ImageIO.write(bi, "bmp", sfile);
                else
                    ImageIO.write(bi, sfile.getName().substring(rns), sfile);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "写入文件失败: " + e);
            }
        }
    }

    /**
     * Open a file
     *
     * @param evt Event
     */
    private void fileOpenActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "jpg", "jpeg", "gif", "bmp", "png");
        fileChooser.setFileFilter(filter);
        int rVal = fileChooser.showOpenDialog(this);
        System.setProperty("user.dir", fileChooser.getCurrentDirectory().getAbsolutePath());
        if (rVal == JFileChooser.APPROVE_OPTION) {
            sfile = fileChooser.getSelectedFile();
            loadImage(sfile);
        }
    }

    void loadImage(File sfile) {
        this.sfile = sfile;
        try {
            bi = ImageIO.read(sfile);
            if (bi == null) {
                // 处理文件格式不支持的情况
                JOptionPane.showMessageDialog(this, "不支持的文件格式或文件损坏: " + sfile.getName(), "加载错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            transform = new Transform(bi);  // 假设 Transform 构造器不会抛出异常
            newImage();  // 假设这个方法用于更新UI或其他逻辑
        } catch (IOException e) {
            // 处理读取文件时的IO异常
            JOptionPane.showMessageDialog(this, "加载文件失败: " + e.getMessage(), "IO错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            // 处理其他可能的异常
            JOptionPane.showMessageDialog(this, "加载图片时发生错误: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Reset settings for a new image
     */
    private void newImage() {
        nowShowing.setText(transform.getText());
        dp.setImage(transform.getImage());
        dp.setSize(transform.getImage().getWidth(), transform.getImage().getHeight());
        dp.setPreferredSize(new Dimension(transform.getImage().getWidth(), transform.getImage().getHeight()));
        this.setMaximumSize(getToolkit().getScreenSize());
        zoomSlider.setValue(100);
        dp.apply(100);
        scrollPane.revalidate();
        repaint();
    }

    /**
     * Update the image being shown for new transform
     */
    private void updateImage() {
        nowShowing.setText(transform.getText());
        dp.setImage(transform.getImage());
        repaint();
    }

    private StegSolve(File file) {
        that = this;
        initComponents();
        if (file != null) {
            loadImage(file);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            if (args.length > 0) {
                new StegSolve(new File(args[0])).setVisible(true);
            } else {
                new StegSolve(null).setVisible(true);
            }
        });
    }
}
