package com.jbooktrader.platform.model;

import com.jbooktrader.platform.backtest.*;
import com.jbooktrader.platform.chart.*;
import com.jbooktrader.platform.dialog.*;
import com.jbooktrader.platform.optimizer.*;
import static com.jbooktrader.platform.preferences.JBTPreferences.*;
import com.jbooktrader.platform.preferences.*;
import com.jbooktrader.platform.startup.*;
import com.jbooktrader.platform.strategy.*;
import com.jbooktrader.platform.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

/**
 * Acts as a controller in the Model-View-Controller pattern
 */
public class MainFrameController {
    private final MainFrameDialog mainViewDialog;
    private final JTable strategyTable;
    private final StrategyTableModel strategyTableModel;
    private final Dispatcher dispatcher;

    public MainFrameController(String runMode) throws JBookTraderException {
        mainViewDialog = new MainFrameDialog();
        dispatcher = Dispatcher.getInstance();
        dispatcher.addListener(mainViewDialog);
        strategyTable = mainViewDialog.getStrategyTable();
        strategyTableModel = mainViewDialog.getStrategyTableModel();
        assignListeners();
        
        if(runMode != null){
        	System.out.print("Starting in mode: " + runMode);
        	System.out.print("Matches?: " + Mode.Trade.toString());

        }
        if(runMode.equalsIgnoreCase(Mode.Trade.toString())){
        	for(int i = 0; i < strategyTableModel.rows.size(); i++){
                try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    Strategy strategy = strategyTableModel.createStrategyForRow(i);
                    dispatcher.setMode(Mode.Trade);
                    dispatcher.getTrader().getAssistant().addStrategy(strategy);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                }
                finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
        	}
        }else if(runMode.equalsIgnoreCase(Mode.ForwardTest.toString())){
        	for(int i = 0; i < strategyTableModel.rows.size(); i++){
                try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    Strategy strategy = strategyTableModel.createStrategyForRow(i);
                    dispatcher.setMode(Mode.ForwardTest);
                    dispatcher.getTrader().getAssistant().addStrategy(strategy);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                }
                finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
        	}        
        }else if(runMode.equalsIgnoreCase(Mode.ClosingPositions.toString())){
        	mainViewDialog.closeAllPositionsAction(null);
        }else{
        	dispatcher.setMode(Mode.Idle);
        }

    }

    private void exit() {
        String question = "Are you sure you want to exit " + AtomicTrader.APP_NAME + "?";
        int answer = JOptionPane.showConfirmDialog(mainViewDialog, question, AtomicTrader.APP_NAME, JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            PreferencesHolder prefs = PreferencesHolder.getInstance();
            prefs.set(MainWindowWidth, mainViewDialog.getSize().width);
            prefs.set(MainWindowHeight, mainViewDialog.getSize().height);
            dispatcher.exit();
        }
    }

    private Strategy createSelectedRowStrategy() throws JBookTraderException {
        int selectedRow = strategyTable.getSelectedRow();
        if (selectedRow < 0) {
            throw new JBookTraderException("No strategy is selected.");
        }
        return strategyTableModel.createStrategyForRow(selectedRow);
    }

    private void openURL(String url) {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URI(url));
        } catch (Throwable t) {
            dispatcher.getEventReport().report(t);
            MessageDialog.showException(t);
        }
    }

    private void assignListeners() {

        strategyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int modifiers = e.getModifiers();
                boolean actionRequested = (modifiers & InputEvent.BUTTON2_MASK) != 0;
                actionRequested = actionRequested || (modifiers & InputEvent.BUTTON3_MASK) != 0;
                if (actionRequested) {
                    int selectedRow = strategyTable.rowAtPoint(e.getPoint());
                    strategyTable.setRowSelectionInterval(selectedRow, selectedRow);
                    mainViewDialog.showPopup(e);
                    strategyTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        });

        mainViewDialog.informationAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    int selectedRow = strategyTable.getSelectedRow();
                    if (selectedRow < 0) {
                        throw new JBookTraderException("No strategy is selected.");
                    }

                    Strategy strategy = strategyTableModel.getStrategyForRow(selectedRow);
                    if (strategy == null) {
                        String name = strategyTableModel.getStrategyNameForRow(selectedRow);
                        strategy = ClassFinder.getInstance(name);
                    }

                    new StrategyInformationDialog(mainViewDialog, strategy);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        mainViewDialog.backTestAllAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	for(int i = 0; i < strategyTableModel.rows.size(); i++){
	                try {
	                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	                    Strategy strategy = strategyTableModel.createStrategyForRow(i);
	                    dispatcher.setMode(Mode.BackTest);
	                    new BackTestDialog(mainViewDialog, strategy);
	                } catch (Throwable t) {
	                    MessageDialog.showException(t);
	                } finally {
	                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                }
            	}
            }
        });
        
        mainViewDialog.backTestAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    dispatcher.getTrader().getAssistant().removeAllStrategies();
                    Strategy strategy = createSelectedRowStrategy();
                    dispatcher.setMode(Mode.BackTest);
                    new BackTestDialog(mainViewDialog, strategy);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        mainViewDialog.optimizeAction(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    int selectedRow = strategyTable.getSelectedRow();
                    if (selectedRow < 0) {
                        throw new JBookTraderException("No strategy is selected.");
                    }
                    String name = strategyTableModel.getStrategyNameForRow(selectedRow);
                    dispatcher.setMode(Mode.Optimization);
                    OptimizerDialog optimizerDialog = new OptimizerDialog(mainViewDialog, name);
                    optimizerDialog.setVisible(true);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        
        mainViewDialog.forwardTestAllAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	for(int i = 0; i < strategyTableModel.rows.size(); i++){
	                try {
	                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	                    Strategy strategy = strategyTableModel.createStrategyForRow(i);
	                    dispatcher.setMode(Mode.ForwardTest);
	                    dispatcher.getTrader().getAssistant().addStrategy(strategy);
	                } catch (Throwable t) {
	                    MessageDialog.showException(t);
	                }
	                finally {
	                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                }
            	}
            }
        });

        mainViewDialog.forwardTestAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    Strategy strategy = createSelectedRowStrategy();
                    dispatcher.setMode(Mode.ForwardTest);
                    dispatcher.getTrader().getAssistant().addStrategy(strategy);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        mainViewDialog.tradeAllAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	for(int i = 0; i < strategyTableModel.rows.size(); i++){
	                try {
	                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	                    Strategy strategy = strategyTableModel.createStrategyForRow(i);
	                    dispatcher.setMode(Mode.Trade);
	                    dispatcher.getTrader().getAssistant().addStrategy(strategy);
	                } catch (Throwable t) {
	                    MessageDialog.showException(t);
	                }
	                finally {
	                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	                }
            	}
            }
        });
        
        mainViewDialog.tradeAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    Strategy strategy = createSelectedRowStrategy();
                    dispatcher.setMode(Mode.Trade);
                    dispatcher.getTrader().getAssistant().addStrategy(strategy);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        
        mainViewDialog.stopAllAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    dispatcher.getTrader().getAssistant().removeAllStrategies();
                    dispatcher.setMode(Mode.Idle);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        
        mainViewDialog.stopAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    Strategy strategy = createSelectedRowStrategy();
                    dispatcher.getTrader().getAssistant().removeStrategy(strategy);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        
        mainViewDialog.closeAllPositionsAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	//TODO:implement
            }
        });
        mainViewDialog.closePositionAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	//TODO:implement
            }
        });
        mainViewDialog.resetAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	//TODO:implement
            	try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    dispatcher.getTrader().getAssistant().removeAllStrategies();
                    dispatcher.setMode(Mode.Idle);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        mainViewDialog.chartAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    int selectedRow = strategyTable.getSelectedRow();
                    if (selectedRow < 0) {
                        MessageDialog.showMessage("No strategy is selected.");
                        return;
                    }

                    Strategy strategy = strategyTableModel.getStrategyForRow(selectedRow);
                    if (strategy == null) {
                        String msg = "Please run this strategy first.";
                        MessageDialog.showMessage(msg);
                        return;
                    }

                    PerformanceChartData pcd = strategy.getPerformanceManager().getPerformanceChartData();
                    if (pcd == null || pcd.isEmpty()) {
                        String msg = "There is no data to chart. Please run a back test first.";
                        MessageDialog.showMessage(msg);
                        return;
                    }

                    PerformanceChart spChart = new PerformanceChart(mainViewDialog, strategy);
                    JFrame chartFrame = spChart.getChart();
                    chartFrame.setVisible(true);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        mainViewDialog.preferencesAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    new PreferencesDialog(mainViewDialog);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                } finally {
                    mainViewDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });


        mainViewDialog.discussionAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openURL("http://groups.google.com/group/jbooktrader/topics?gvc=2");
            }
        });

        mainViewDialog.releaseNotesAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openURL("http://code.google.com/p/jbooktrader/wiki/ReleaseNotes");
            }
        });

        mainViewDialog.userManualAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openURL("http://docs.google.com/View?id=dfzgvqp4_10gb63b8hg");
            }
        });

        mainViewDialog.projectHomeAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openURL("http://code.google.com/p/jbooktrader/");
            }
        });

        mainViewDialog.exitAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });

        mainViewDialog.exitAction(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        mainViewDialog.aboutAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    new AboutDialog(mainViewDialog);
                } catch (Throwable t) {
                    MessageDialog.showException(t);
                }
            }
        });
    }
}
