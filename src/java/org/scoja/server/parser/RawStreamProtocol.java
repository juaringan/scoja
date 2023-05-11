/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, S.A.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.scoja.server.parser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.scoja.server.core.Link;

import org.scoja.trans.RemoteInfo;
import org.scoja.beep.common.BEEPException;
import org.scoja.beep.common.Profiles;
import org.scoja.beep.common.ReplyCodes;
import org.scoja.beep.easy.*;
import org.scoja.beep.frame.*;
import org.scoja.beep.msg.*;


public class RawStreamProtocol implements StreamProtocol, BEEPPeer {
    
    private static final GreetingResponse GREETING 
        = new GreetingResponse(new String[] {
            Profiles.SYSLOG_RAW,
        });
        
    private static final String INVITATION
        = "This is Scoja server, ready to log your data.";

    
    protected final RemoteInfo peer;
    protected final Link link;
    protected final OutputStream exit;
    protected final EventSplitter splitter;
    protected byte[] data;
    protected ByteBuffer bdata;
    protected final BEEPSession session;
    protected final BEEPDecoder entry;
    
    public RawStreamProtocol(final RemoteInfo peer, final Link link,
                             final OutputStream exit) {
        this.peer = peer;
        this.link = link;
        this.exit = exit;
        this.splitter = new EventSplitter(peer, link);
        this.data = new byte[16*1024];
        this.bdata = ByteBuffer.wrap(data);
        
        BEEPsitor out0 = new FastBEEPEncoder(exit);
        //final BEEPsitor out0b = new FastBEEPEncoder(
        //    new LinerOutputStream("< ", screen));
        //out0 = new BEEPsitorBroadcaster(out0, out0b);
        final BEasytor out1 = new FreeMessageEncoder(out0);
        session = new BEEPSession(this, out1, true);
        final BEasytor in2 = new AcknowledgeOnArrival(
            session.getInPort(), out1);
        BEEPsitor in1 = new MessageComposer(in2);
        //final BEEPsitor in1b = new FastBEEPEncoder(
        //    new LinerOutputStream("> ", screen));
        //in1 = new BEEPsitorBroadcaster(in1b, in1);
        //in1 = new AnswerMatching(in1);
        entry = new BEEPDecoder(in1);
        session.greeting(GREETING);
    }
    
    public void setInBuffer(final int inbuffer) {
        splitter.setInBuffer(inbuffer);
        if (inbuffer != data.length) {
            data = new byte[inbuffer];
            bdata = ByteBuffer.wrap(data);
        }
    }

    public void processAvailable(final ReadableByteChannel source)
    throws IOException {
        for (;;) {
            bdata.position(0);
            final int readed = source.read(bdata);
            if (readed <= 0) {
                if (readed < 0) source.close();
                break;
            }
            entry.consider(data, 0, readed);
        }
    }
    
    public void flush() {
        splitter.flush();
    }
    

    //======================================================================
    public void greeting(final GreetingResponse m) {
        //System.err.println("GREETING: " + m);
    }

    public void openChannelRequest(final OpenRequest m) {
        //System.err.println("REQUEST: " + m);
        session.openChannelConfirmation(
            new OpenConfirmation(m, m.getProfiles().get(0).getURI()));
        startData(m.getChannel());
    }
    
    public void openChannelConfirmation(final OpenConfirmation m) {
        //System.err.println("CONFIRMATION: " + m);
        //This should not happen because we are not requesting new channels.
    }
    
    public void closeChannelRequest(final CloseRequest m) {
        //System.err.println("REQUEST: " + m);
        //Should not happen, because it is supposed that I, the listener,
        // close channels.
        session.denyRequest(
            new ErrorResponse(m, ReplyCodes.ACTION_IMPOSSIBLE));
    }
    
    public void closeChannelConfirmation(final CloseConfirmation m) {
        //System.err.println("CONFIRMATION: " + m);
    }
    
    public void closeSessionRequest(final CloseRequest m) {
        //System.err.println("REQUEST: " + m);
        session.closeSessionConfirmation(new CloseConfirmation(m));
    }
    
    public void closeSessionConfirmation(final CloseConfirmation m) {
        //System.err.println("CONFIRMATION: " + m);
        //This should not happen because we are not requesting session end.
    }
    
    public void denyRequest(final ErrorResponse m) {
        //System.err.println("CONFIRMATION: " + m);
        //This should happen only as a response to a closeChannelRequest
        final ChannelManagementRequest cause = m.getCause();
        if (cause instanceof CloseRequest) {
            //Reuse the channel
            final CloseRequest req = (CloseRequest)cause;
            startData(req.getChannel());
        }
    }
    
    public void message(final Message msg) {
        //System.err.println("MESSAGE: " + msg);
        session.error(Message.error().responding(msg)
                      .utf8("Expecting no incoming message"));
    }

    public void error(final Message msg) {
        //System.err.println("ERROR: " + msg);
        finishData(msg.getChannel());
    }

    public void reply(final Message msg) {
        //System.err.println("REPLY: " + msg);
        finishData(msg.getChannel());
    }

    public void answer(final Message msg) {
        splitter.add(msg.getPayload());
        splitter.flush();
    }

    public void nul(final Message msg) {
        //System.err.println("NUL: " + msg);
        finishData(msg.getChannel());
    }
    
    public void window(final int channel, final int ackno, final int window) {
        //Nothing to do.
    }

    protected void startData(final int channel) {
        session.message(
            Message.message().setChannel(channel).utf8(INVITATION));
    }
    
    protected void finishData(final int channel) {
        session.closeChannelRequest(
            new CloseRequest(channel, ReplyCodes.SUCCESS));
    }
}
