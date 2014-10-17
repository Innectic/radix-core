/*******************************************************************************
 * RDXServerBridge.java
 * Copyright (c) 2014 WildBamaBoy.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MCA Minecraft Mod license.
 ******************************************************************************/

package com.radixshock.radixcore.frontend;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import com.radixshock.radixcore.core.Constants;

public final class RDXServerBridge
{
	public static String sendVersionQuery()
	{
		String version = null;

		try
		{
			final Socket connectSocket = new Socket("107.170.27.20", 3577);
			connectSocket.setSoTimeout(2000);
			
			final DataOutputStream dataOut = new DataOutputStream(connectSocket.getOutputStream());
			final DataInputStream dataIn = new DataInputStream(connectSocket.getInputStream());

			dataOut.writeByte(1);
			dataOut.writeUTF("@Validate@");
			dataOut.writeUTF("RDX");
			dataOut.writeUTF(Constants.VERSION);
			
			version = dataIn.readUTF();
			
			connectSocket.close();
		}

		catch (final Throwable e)
		{
			e.printStackTrace();
		}

		return version;
	}

	public static void sendCrashReport(String completeReport, boolean isServer)
	{
		try
		{
			if (completeReport.contains("at com.radixshock."))
			{
				final Socket connectSocket = new Socket("107.170.27.20", 3577);
				connectSocket.setSoTimeout(2000);
				
				final DataOutputStream dataOut = new DataOutputStream(connectSocket.getOutputStream());
				new DataInputStream(connectSocket.getInputStream());

				dataOut.writeByte(2);
				dataOut.writeUTF("@Validate@");
				dataOut.writeUTF("RDX");
				dataOut.writeUTF(Constants.VERSION);
				dataOut.writeBoolean(isServer);
				dataOut.writeUTF(completeReport);

				connectSocket.close();
			}
		}

		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}
}
