package com.jbooktrader.platform.model;


import com.jbooktrader.platform.model.ModelListener.*;
import com.jbooktrader.platform.report.*;
import com.jbooktrader.platform.startup.*;
import com.jbooktrader.platform.trader.*;
import com.jbooktrader.platform.util.*;
import com.jbooktrader.platform.web.*;

import java.io.*;
import java.util.*;

/**
 * Acts as the dispatcher of the services.
 */
public class Dispatcher {
    private static Dispatcher instance;
    private final List<ModelListener> listeners;
    private EventReport eventReport;
    private Trader trader;
    private NTPClock ntpClock;
    private Mode mode;

    private Dispatcher() {
        listeners = new ArrayList<ModelListener>();
    }

    public static synchronized Dispatcher getInstance() {
        if (instance == null) {
            instance = new Dispatcher();
        }
        return instance;
    }

    public void setReporter() throws IOException {
        eventReport = new EventReport();
    }

    public void addListener(ModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ModelListener listener) {
        listeners.remove(listener);
    }

    public void fireModelChanged(Event event, Object value) {
        for (ModelListener listener : listeners) {
            try {
                listener.modelChanged(event, value);
            } catch (Exception e) {
                eventReport.report(e);
            }
        }
    }

    public void fireModelChanged(Event event) {
        fireModelChanged(event, null);
    }


    public synchronized Trader getTrader() {
        if (trader == null) {
            trader = new Trader();
        }
        return trader;
    }

    public NTPClock getNTPClock() {
        return ntpClock;
    }

    public EventReport getEventReport() {
        return eventReport;
    }

    public Mode getMode() {
        return mode;
    }

    public void exit() {
        if (trader != null && mode!=Mode.Idle) {
        	trader.getAssistant().removeAllStrategies();
        	long delay = 2000;
            try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            //while(trader.getAssistant().isCancelingAllOpenOrders){}
        	//trader.getAssistant().disconnect();
        }
        System.exit(0);
    }

    public void setMode(Mode mode) throws JBookTraderException {
        if (mode == Mode.Trade || mode == Mode.ForwardTest || mode == Mode.ClosingPositions) {
            if (ntpClock == null) {
                ntpClock = new NTPClock();
            }
        }

        if (this.mode != mode) {
            eventReport.report(AtomicTrader.APP_NAME, "Running mode changed to: " + mode.getName());
        }

        this.mode = mode;

        // Disable all reporting when JBT runs in optimization mode. The optimizer runs
        // thousands of strategies, and the amount of data to report would be enormous.
        if (mode == Mode.Optimization) {
            eventReport.disable();
        } else {
            eventReport.enable();
        }

        MonitoringServer.start();
        if (mode == Mode.Trade || mode == Mode.ForwardTest || mode == Mode.ClosingPositions) {
            trader.getAssistant().connect();
            //MonitoringServer.start();
        } else {
            //trader.getAssistant().disconnect();
        }

        fireModelChanged(Event.ModeChanged);
    }
}
