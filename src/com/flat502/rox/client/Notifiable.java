package com.flat502.rox.client;

import com.flat502.rox.http.HttpResponseBuffer;

interface Notifiable {
	public void notify(HttpResponseBuffer rsp, ResponseContext context);
	public void notify(Throwable e, ResponseContext context);
	public void notifyTimedOut(Throwable cause, ResponseContext context);
}
