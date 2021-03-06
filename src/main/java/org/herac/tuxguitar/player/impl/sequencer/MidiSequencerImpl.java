package org.herac.tuxguitar.player.impl.sequencer;

import org.apache.log4j.Logger;
import org.herac.tuxguitar.player.base.MidiPlayerException;
import org.herac.tuxguitar.player.base.MidiSequenceHandler;
import org.herac.tuxguitar.player.base.MidiSequencer;
import org.herac.tuxguitar.player.base.MidiTransmitter;
import org.herac.tuxguitar.player.impl.jsa.midiport.MidiPortSynthesizer;

public class MidiSequencerImpl implements MidiSequencer {
  /** The Logger for this class. */
  public static final transient Logger LOG = Logger
      .getLogger(MidiSequencerImpl.class);
  private class MidiTimer extends Thread {

    private static final int TIMER_DELAY = 15;

    private MidiSequencerImpl sequencer;

    public MidiTimer(MidiSequencerImpl sequencer) {
      this.sequencer = sequencer;
    }

    public void run() {
      try {
        synchronized (this.sequencer) {
          while (this.sequencer.process()) {
            this.sequencer.wait(TIMER_DELAY);
          }
        }
      } catch (Throwable throwable) {
        LOG.error(throwable);
      }
    }
  }

  private MidiEventDispacher midiEventDispacher;
  private MidiEventPlayer midiEventPlayer;
  private MidiTickPlayer midiTickPlayer;
  private MidiTrackController midiTrackController;
  private boolean reset;
  private boolean running;
  private boolean stopped;

  private MidiTransmitter transmitter;

  public MidiSequencerImpl() {
    this.running = false;
    this.stopped = true;
    this.midiTickPlayer = new MidiTickPlayer();
    this.midiEventPlayer = new MidiEventPlayer(this);
    this.midiEventDispacher = new MidiEventDispacher(this);
    this.midiTrackController = new MidiTrackController(this);
  }

  public synchronized void addEvent(MidiEvent event) {
    this.midiEventPlayer.addEvent(event);
    this.midiTickPlayer.notifyTick(event.getTick());
  }

  public void check() {
    // Not implemented
  }

  public synchronized void close() throws MidiPlayerException {
    if (isRunning()) {
      this.stop();
    }
  }

  public synchronized MidiSequenceHandler createSequence(int tracks)
      throws MidiPlayerException {
    return new MidiSequenceHandlerImpl(this, tracks);
  }

  public String getKey() {
    return "tuxguitar.sequencer";
  }

  public synchronized MidiTrackController getMidiTrackController() {
    return this.midiTrackController;
  }

  public String getName() {
    return "TuxGuitar Sequencer";
  }

  public synchronized long getTickLength() {
    return this.midiTickPlayer.getTickLength();
  }

  public synchronized long getTickPosition() {
    return this.midiTickPlayer.getTick();
  }

  public synchronized MidiTransmitter getTransmitter() {
    return this.transmitter;
  }

  public synchronized boolean isRunning() {
    return this.running;
  }

  public synchronized void open() {
    // not implemented
  }

  protected synchronized boolean process() throws MidiPlayerException {
    boolean running = this.isRunning();
    if (running) {
      if (this.reset) {
        this.reset(false);
        this.reset = false;
        this.midiEventPlayer.reset();
      }
      this.stopped = false;
      this.midiTickPlayer.process();
      this.midiEventPlayer.process();
      if (this.getTickPosition() > this.getTickLength()) {
        this.stop();
      }
    } else if (!this.stopped) {
      this.stopped = true;
      this.midiEventPlayer.clearEvents();
      this.midiTickPlayer.clearTick();
      this.reset(true);
    }
    return running;
  }

  public synchronized void reset(boolean systemReset)
      throws MidiPlayerException {
    this.getTransmitter().sendAllNotesOff();
    for (int channel = 0; channel < 16; channel++) {
      this.getTransmitter().sendPitchBend(channel, 64);
    }
    if (systemReset) {
      this.getTransmitter().sendSystemReset();
    }
  }

  public synchronized void sendEvent(MidiEvent event)
      throws MidiPlayerException {
    if (!this.reset) {
      this.midiEventDispacher.dispatch(event);
    }
  }

  public synchronized void setMute(int index, boolean mute)
      throws MidiPlayerException {
    this.getMidiTrackController().setMute(index, mute);
  }

  public synchronized void setRunning(boolean running)
      throws MidiPlayerException {
    this.running = running;
    if (this.running) {
      this.setTempo(120);
      this.setTickPosition(this.getTickPosition());
      new MidiTimer(this).start();
    } else {
      this.process();
    }
  }

  public synchronized void setSolo(int index, boolean solo)
      throws MidiPlayerException {
    this.getMidiTrackController().setSolo(index, solo);
  }

  public synchronized void setTempo(int tempo) {
    this.midiTickPlayer.setTempo(tempo);
  }

  public synchronized void setTickPosition(long tickPosition) {
    this.reset = true;
    this.midiTickPlayer.setTick(tickPosition);
  }

  public synchronized void setTransmitter(MidiTransmitter transmitter) {
    this.transmitter = transmitter;
  }

  public synchronized void start() throws MidiPlayerException {
    this.setRunning(true);
  }

  public synchronized void stop() throws MidiPlayerException {
    this.setRunning(false);
  }
}
