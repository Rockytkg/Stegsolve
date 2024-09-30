/*
 * FrameBrowser.java
 */

package stegsolve;

import javax.swing.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.swing.filechooser.*;

// todo: jphide checker/ invisible secrets

/**
 * Frame Browser
 *
 * @author Caesum
 */
public class FrameBrowser extends JFrame {
    /**
     * Label with the text showing the frame number
     */
    private JLabel nowShowing;
    /**
     * Panel with image on it
     */
    private DPanel dp;
    /**
     * Scroll pane for the image
     */
    private JScrollPane scrollPane;

    /**
     * The image being viewed
     */
    private final BufferedImage bi;
    /**
     * The individual frames of the image
     */
    private java.util.List<BufferedImage> frames = null;
    /**
     * Number of the current frame
     */
    private int fnum = 0;
    /**
     * Number of frames
     */
    private int numframes = 0;

    /**
     * Creates a new frame browser
     *
     * @param b The image the view
     * @param f The file of the image
     */
    public FrameBrowser(BufferedImage b, File f) {
        BufferedImage bnext;
        bi = b;
        initComponents();
        fnum = 0;
        numframes = 0;
        frames = new ArrayList<>();
        try {
            ImageInputStream ii = ImageIO.createImageInputStream(f);
            if (ii == null) System.out.println("Couldn't create input stream");
            ImageReader rr = null;
            if (ii != null) {
                rr = ImageIO.getImageReaders(ii).next();
            }
            if (rr == null) System.out.println("No image reader");
            Objects.requireNonNull(rr).setInput(ii);
            int fread = rr.getMinIndex();
            while (true) {
                bnext = rr.read(numframes + fread);
                if (bnext == null)
                    break;
                frames.add(bnext);
                numframes++;
            }
            System.out.println("总帧数 " + numframes);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to load file: " + e);
        } catch (IndexOutOfBoundsException e) {
            // expected for reading too many frames
        }
        newImage();
    }

    // <editor-fold defaultstate="collapsed" desc="Initcomponents()">
    private void initComponents() {

        nowShowing = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());

        this.add(nowShowing, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        JButton backwardButton = new JButton("<");
        backwardButton.addActionListener(this::backwardButtonActionPerformed);
        JButton forwardButton = new JButton(">");
        forwardButton.addActionListener(this::forwardButtonActionPerformed);
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(this::saveButtonActionPerformed);
        buttonPanel.add(backwardButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);

        backwardButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "back");
        backwardButton.getActionMap().put("back", backButtonPress);
        forwardButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "forward");
        forwardButton.getActionMap().put("forward", forwardButtonPress);

        dp = new DPanel();
        scrollPane = new JScrollPane(dp);
        add(scrollPane, BorderLayout.CENTER);

        pack();
        //setResizable(false);
    }// </editor-fold>

    /**
     * This is used to map the left arrow key to the back button
     */
    private final Action backButtonPress = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            backwardButtonActionPerformed(e);
        }
    };

    /**
     * Move back by one frame
     *
     * @param evt Event
     */
    private void backwardButtonActionPerformed(ActionEvent evt) {
        fnum--;
        if (fnum < 0) fnum = numframes - 1;
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
     * Move forward by one frame
     *
     * @param evt Event
     */
    private void forwardButtonActionPerformed(ActionEvent evt) {
        fnum++;
        if (fnum >= numframes) fnum = 0;
        updateImage();
    }

    /**
     * Save the current frame
     *
     * @param evt Event
     */
    private void saveButtonActionPerformed(ActionEvent evt) {
        File sfile;
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Images", "jpg", "gif", "png", "bmp");
        fileChooser.setFileFilter(filter);
        fileChooser.setSelectedFile(new File("frame" + (fnum + 1) + ".bmp"));
        int rVal = fileChooser.showSaveDialog(this);
        System.setProperty("user.dir", fileChooser.getCurrentDirectory().getAbsolutePath());
        if (rVal == JFileChooser.APPROVE_OPTION) {
            sfile = fileChooser.getSelectedFile();
            try {
                BufferedImage bbx = frames.get(fnum);
                int rns = sfile.getName().lastIndexOf(".") + 1;
                if (rns == 0)
                    ImageIO.write(bbx, "bmp", sfile);
                else
                    ImageIO.write(bbx, sfile.getName().substring(rns), sfile);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to write file: " + e);
            }
        }
    }

    /**
     * Update the text description and repaint the image
     */
    private void updateImage() {
        nowShowing.setText(String.format("帧：%d / %d", fnum + 1, numframes));
        if (numframes == 0) return;
        dp.setImage(frames.get(fnum));
        repaint();
    }

    /**
     * Show the image and make sure the frame browser looks right
     */
    private void newImage() {
        nowShowing.setText(String.format("帧：%d / %d", fnum + 1, numframes));
        if (numframes == 0) return;
        dp.setImage(frames.get(fnum));
        dp.setSize(bi.getWidth(), bi.getHeight());
        dp.setPreferredSize(new Dimension(bi.getWidth(), bi.getHeight()));
        this.setMaximumSize(getToolkit().getScreenSize());
        pack();
        dp.apply(100);
        scrollPane.revalidate();
        repaint();
        this.setSize(500, 600);
    }

}
