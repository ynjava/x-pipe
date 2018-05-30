package com.ctrip.xpipe.redis.proxy.handler;

import com.ctrip.xpipe.redis.core.proxy.DefaultProxyProtocol;
import com.ctrip.xpipe.redis.core.proxy.DefaultProxyProtocolParser;
import com.ctrip.xpipe.redis.proxy.AbstractNettyTest;
import com.ctrip.xpipe.redis.proxy.exception.ResourceIncorrectException;
import com.ctrip.xpipe.redis.proxy.session.DefaultFrontendSession;
import com.ctrip.xpipe.redis.proxy.tunnel.DefaultTunnel;
import com.ctrip.xpipe.redis.proxy.tunnel.DefaultTunnelManager;
import com.ctrip.xpipe.redis.proxy.tunnel.state.FrontendClosed;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.charset.Charset;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author chen.zhu
 * <p>
 * May 29, 2018
 */
public class FrontendSessionNettyHandlerTest extends AbstractNettyTest {

    @Mock
    private DefaultTunnelManager manager;

    @Mock
    private DefaultTunnel tunnel;

    @Mock
    private DefaultFrontendSession session;

    private FrontendSessionNettyHandler handler;

    private EmbeddedChannel channel;

    @Before
    public void beforeFrontendSessionNettyHandlerTest() {
        MockitoAnnotations.initMocks(this);
        handler = new FrontendSessionNettyHandler(manager);
        handler.setTunnel(tunnel);
        handler.setSession(session);
        when(manager.getOrCreate(any(), any())).thenReturn(tunnel);
        when(tunnel.frontend()).thenReturn(session);
        channel = new EmbeddedChannel(handler);
    }

    @Test
    public void setTunnelStateWhenSessionClosed() {
        handler.setTunnelStateWhenSessionClosed();
        verify(tunnel).setState(new FrontendClosed(tunnel));
    }

    @Test
    public void testChannelInactive() {
        channel.finish();
        verify(tunnel).setState(new FrontendClosed(tunnel));
    }

    @Test
    public void channelRead() {
        channel.writeInbound(new DefaultProxyProtocolParser().read("PROXY ROUTE TCP://127.0.0.1:6379"));
        verify(manager).getOrCreate(any(), any());
        verify(tunnel).frontend();
    }

    @Test
    public void testChannelRead2() {
        String expected = "Hello World";
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object object = invocation.getArguments()[0];
                Assert.assertTrue(object instanceof ByteBuf);
                Assert.assertEquals(expected, ((ByteBuf)object).toString(Charset.defaultCharset()));
                return null;
            }
        }).when(tunnel).forwardToBackend(any());
        channel.writeInbound(Unpooled.copiedBuffer(expected.getBytes()));
        verify(manager, never()).getOrCreate(any(), any());
        verify(tunnel).forwardToBackend(any());
    }

    @Test
    public void testChannelRead3() {
        when(tunnel.forwardToBackend(any())).thenReturn(null);
        channel.writeInbound("Hello Wrold");
        verify(manager, never()).getOrCreate(any(), any());
        verify(tunnel, never()).forwardToBackend(any());
    }

}