/*******************************************************************************
 * AbstractPacketCodec.java
 * Copyright (c) 2014 Radix-Shock Entertainment.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/

package com.radixshock.radixcore.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.radixshock.radixcore.core.IEnforcedCore;
import com.radixshock.radixcore.core.RadixCore;

/**
 * Base class to define a packet codec: COder-DECoder.
 */
public abstract class AbstractPacketCodec
{
	private final IEnforcedCore	mod;

	/**
	 * Constructor
	 * 
	 * @param mod
	 *            The mod this codec belongs to.
	 */
	public AbstractPacketCodec(IEnforcedCore mod)
	{
		this.mod = mod;
	}

	/**
	 * Encodes the provided packet's data into the buffer.
	 * 
	 * @param packet
	 *            The packet containing data to be encoded.
	 * @param context
	 *            Contains the packet's info such as channel, pipeline, etc.
	 * @param buffer
	 *            Buffer used to convert packet data into bytes.
	 */
	public abstract void encode(Packet packet, ChannelHandlerContext context, ByteBuf buffer);

	/**
	 * Decodes the provided buffer's data and places it back into the packet.
	 * 
	 * @param packet
	 *            The packet that will be filled with data.
	 * @param context
	 *            Contains the packet's info such as channel, pipeline, etc.
	 * @param buffer
	 *            Buffer containing data to be decoded.
	 */
	public abstract void decode(Packet packet, ChannelHandlerContext context, ByteBuf buffer);

	/**
	 * Writes the provided object to the provided ByteBuf.
	 * 
	 * @param buffer
	 *            The ByteBuf that the object should be written to.
	 * @param object
	 *            The object to write to the buffer.
	 */
	protected void writeObject(ByteBuf buffer, Object object)
	{
		writeByteArray(buffer, convertToByteArray(object));
	}

	/**
	 * Reads the next object from the provided ByteBuf.
	 * 
	 * @param buffer
	 *            The ByteBuf containing the object to be read.
	 * 
	 * @return Object form of the object read. Must be cast to expected type.
	 */
	protected Object readObject(ByteBuf buffer)
	{
		return convertBytesToObject(readByteArray(buffer));
	}

	/**
	 * Converts the provided object to a byte array that can be written to the
	 * buffer.
	 * 
	 * @param obj
	 *            The object to be converted to a byte array.
	 * 
	 * @return The object's byte array representation.
	 */
	private byte[] convertToByteArray(Object obj)
	{
		final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

		try
		{
			final ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeObject(obj);
			objectOutput.close();
		}

		catch (final IOException e)
		{
			mod.getLogger().log(e);
		}

		return byteOutput.toByteArray();
	}

	/**
	 * Converts the provided byte array back into an Object.
	 * 
	 * @param byteArray
	 *            The byte array to be converted.
	 * 
	 * @return Object form of the provided byte array. Must be cast to expected
	 *         type.
	 */
	private Object convertBytesToObject(byte[] byteArray)
	{
		final ByteArrayInputStream byteInput = new ByteArrayInputStream(byteArray);
		Object returnObject = null;

		try
		{
			final ObjectInputStream objectInput = new ObjectInputStream(byteInput);

			returnObject = objectInput.readObject();
			objectInput.close();
		}

		catch (final IOException e)
		{
			mod.getLogger().log(e);
		}

		catch (final ClassNotFoundException e)
		{
			mod.getLogger().log(e);
		}

		return returnObject;
	}

	/**
	 * Compresses the data in a byte array.
	 * 
	 * @param input
	 *            The byte array to be compressed.
	 * 
	 * @return The byte array in its compressed form.
	 */
	private byte[] compress(byte[] input)
	{
		try
		{
			final Deflater deflater = new Deflater();
			deflater.setLevel(Deflater.BEST_COMPRESSION);
			deflater.setInput(input);

			final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(input.length);
			deflater.finish();

			final byte[] buffer = new byte[1024];

			while (!deflater.finished())
			{
				final int count = deflater.deflate(buffer);
				byteOutput.write(buffer, 0, count);
			}

			byteOutput.close();
			return byteOutput.toByteArray();
		}

		catch (final IOException e)
		{
			RadixCore.getInstance().quitWithException("Error compressing byte array.", e);
			return null;
		}
	}

	/**
	 * Decompresses a compressed byte array.
	 * 
	 * @param input
	 *            The byte array to be decompressed.
	 * 
	 * @return The byte array in its decompressed, readable form.
	 */
	private byte[] decompress(byte[] input)
	{
		try
		{
			final Inflater inflater = new Inflater();
			final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(input.length);
			final byte[] buffer = new byte[1024];
			inflater.setInput(input);

			while (!inflater.finished())
			{
				final int count = inflater.inflate(buffer);
				byteOutput.write(buffer, 0, count);
			}

			byteOutput.close();
			return byteOutput.toByteArray();
		}

		catch (final DataFormatException e)
		{
			RadixCore.getInstance().quitWithException("Error decompressing byte array.", e);
			return null;
		}

		catch (final IOException e)
		{
			RadixCore.getInstance().quitWithException("Error decompressing byte array.", e);
			return null;
		}
	}

	/**
	 * Writes the provided byte array to the buffer. Compresses the provided
	 * array and precedes it data with its length as an int. Then the compressed
	 * array itself is written.
	 * 
	 * @param buffer
	 *            ByteBuf that the byte array will be written to.
	 * @param byteArray
	 *            The byte array that will be written to the ByteBuf.
	 */
	private void writeByteArray(ByteBuf buffer, byte[] byteArray)
	{
		final byte[] compressedArray = compress(byteArray);
		buffer.writeInt(compressedArray.length);
		buffer.writeBytes(compressedArray);
	}

	/**
	 * Reads the next byte array from the buffer. Gets the array's size by
	 * reading the next int, then reads that amount of bytes and returns the
	 * decompressed byte array.
	 * 
	 * @param buffer
	 * 
	 * @return
	 */
	private byte[] readByteArray(ByteBuf buffer)
	{
		final int arraySize = buffer.readInt();
		return decompress(buffer.readBytes(arraySize).array());
	}
}
