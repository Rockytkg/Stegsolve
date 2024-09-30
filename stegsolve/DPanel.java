/*
 * DPanel.java
 */

package stegsolve;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.*;
import java.awt.*;
import java.io.File;
import java.util.TooManyListenersException;
import java.util.List;

/**
 * A JPanel with an image attached to it
 *
 * @author Caesum
 */
public class DPanel extends JPanel {
    private Dimension preferredSize = new Dimension(200, 200);
    private final Dimension defaultSize = new Dimension();
    private final Dimension currentSize = new Dimension();

    private DropTarget dropTarget;
    private DropTargetHandler dropTargetHandler;

    public DPanel() {
        //setBackground(Color.RED);
    }

    /**
     * The image attached to this panel
     */
    private BufferedImage bi = null;

    /**
     * Overridden paint method for the panel which
     * paints the image on the panel
     *
     * @param g graphics object
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bi != null)
            g.drawImage(bi, 0, 0, currentSize.width, currentSize.height, this);
    }

    /**
     * Sets the image for the panel, and calls
     * repaint
     *
     * @param bix Image to show on the panel
     */
    public void setImage(BufferedImage bix) {
        bi = bix;
        defaultSize.width = bi.getWidth();
        defaultSize.height = bi.getHeight();
        setSize(bi.getWidth(), bi.getHeight());
        repaint();
        //apply(100);
    }

    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    public void apply(int percent) {
        currentSize.width = (int) (defaultSize.width * (((float) percent) / 100));
        currentSize.height = (int) (defaultSize.height * (((float) percent) / 100));
        preferredSize = currentSize;
        revalidate();
        repaint();
    }

    protected class DropTargetHandler implements DropTargetListener {

        protected void processDrag(DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY);
            } else {
                dtde.rejectDrag();
            }
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            processDrag(dtde);
            SwingUtilities.invokeLater(new DragUpdate(dtde.getLocation()));
            repaint();
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            processDrag(dtde);
            SwingUtilities.invokeLater(new DragUpdate(dtde.getLocation()));
            repaint();
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {
        }

        @Override
        public void dragExit(DropTargetEvent dte) {
            SwingUtilities.invokeLater(new DragUpdate(null));
            repaint();
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            SwingUtilities.invokeLater(new DragUpdate(null));

            Transferable transferable = dtde.getTransferable();
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrop(dtde.getDropAction());
                try {
                    // 尝试获取数据
                    Object transferDataRaw = transferable.getTransferData(DataFlavor.javaFileListFlavor);

                    // 检查数据类型
                    if (transferDataRaw instanceof List) {
                        @SuppressWarnings("unchecked") // 添加这个注解来抑制未检查的警告
                        List<File> transferData = (List<File>) transferDataRaw;

                        if (transferData.size() == 1) {
                            StegSolve.that.loadImage(transferData.get(0));
                            dtde.dropComplete(true);
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                dtde.rejectDrop();
            }
        }
    }

    protected DropTarget getMyDropTarget() {
        if (dropTarget == null) {
            dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, null);
        }
        return dropTarget;
    }

    protected DropTargetHandler getDropTargetHandler() {
        if (dropTargetHandler == null) {
            dropTargetHandler = new DropTargetHandler();
        }
        return dropTargetHandler;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        try {
            getMyDropTarget().addDropTargetListener(getDropTargetHandler());
        } catch (TooManyListenersException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        getMyDropTarget().removeDropTargetListener(getDropTargetHandler());
    }


    public class DragUpdate implements Runnable {

        public DragUpdate(Point dragPoint) {
        }

        @Override
        public void run() {
            DPanel.this.repaint();
        }
    }
}
