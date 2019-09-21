package com.google.ar.core;

/**
 * @author: crease.xu
 * @version: v1.0
 * @since: 2019-09-20
 * Description:
 *
 * Modification History:
 * -----------------------------------------------------------------------------------
 * Why & What is modified:
 */
public class GetFrameUtil {
  public static Frame getFrame(Session session){
    return new Frame(session);
  }
}
