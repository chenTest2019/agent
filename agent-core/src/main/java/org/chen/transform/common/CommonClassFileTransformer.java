package org.chen.transform.common;

import org.chen.spy.ConfigHelperSpy;
import org.chen.transform.MyClassFileTransformer;

public interface CommonClassFileTransformer extends MyClassFileTransformer {

    String owner = ConfigHelperSpy.class.getName();

}
