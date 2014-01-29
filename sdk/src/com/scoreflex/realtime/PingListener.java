package com.scoreflex.realtime;

import java.util.Map;

/**
 * An interface that contains callbacks used by the {@link Session realtime
 * session} in reply to a Ping request.
 */
public interface PingListener {
  /**
   * This method is called when a reply to a ping request is received before the
   * timeout.
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#ping(PingListener, int)
   *
   * @param latency The latency in milliseconds to receive the ping reply.
   */
  public void onPong(long latency);

  /**
   * This method is called when a ping request timed out.
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#ping(PingListener, int)
   */
  public void onPang();
}
