// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2019
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.framework.osc;

import de.mossgrabers.framework.daw.IHost;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Base class for sending OSC messages to an OSC server.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public abstract class AbstractOpenSoundControlWriter implements IOpenSoundControlWriter
{
    protected final IHost                          host;
    protected final IModel                         model;
    protected final IOpenSoundControlConfiguration configuration;

    protected final IOpenSoundControlClient        oscClient;
    protected final Map<String, Object>            oldValues = new HashMap<> ();

    private final List<IOpenSoundControlMessage>   messages  = new ArrayList<> ();


    /**
     * Constructor.
     *
     * @param host The host
     * @param model The model
     * @param oscClient The OSC client to write to
     * @param configuration The OSC configuration
     */
    protected AbstractOpenSoundControlWriter (final IHost host, final IModel model, final IOpenSoundControlClient oscClient, final IOpenSoundControlConfiguration configuration)
    {
        this.host = host;
        this.model = model;
        this.oscClient = oscClient;
        this.configuration = configuration;
    }


    /**
     * Send all collected messages.
     */
    public void flush ()
    {
        synchronized (this.messages)
        {
            try
            {
                this.logMessages (this.messages);
                this.oscClient.sendBundle (this.messages);
            }
            catch (final IOException ex)
            {
                this.model.getHost ().error ("Could not send UDP message.", ex);
            }

            this.messages.clear ();
        }
    }


    /** {@inheritDoc} */
    @Override
    public void fastSendOSC (final String address, final int [] numbers)
    {
        final List<Object> params = new ArrayList<> ();
        for (final int number: numbers)
            params.add (Integer.valueOf (number));
        this.fastSendOSC (address, params);
    }


    /** {@inheritDoc} */
    @Override
    public void fastSendOSC (final String address)
    {
        this.fastSendOSC (address, Collections.emptyList ());
    }


    protected void fastSendOSC (final String address, final List<Object> parameters)
    {
        this.sendOSC (address, parameters, true);
        this.flush ();
    }


    protected void sendOSCColor (final String address, final double red, final double green, final double blue, final boolean dump)
    {
        final int r = (int) Math.round (red * 255.0);
        final int g = (int) Math.round (green * 255.0);
        final int b = (int) Math.round (blue * 255.0);
        this.sendOSC (address, "rgb(" + r + "," + g + "," + b + ")", dump);
    }


    protected void sendOSC (final String address, final boolean value, final boolean dump)
    {
        this.sendOSC (address, Boolean.valueOf (value), dump);
    }


    protected void sendOSC (final String address, final double value, final boolean dump)
    {
        // Using float here since Double seems to be always received as 0 in Max.
        this.sendOSC (address, Float.valueOf ((float) value), dump);
    }


    protected void sendOSC (final String address, final int value, final boolean dump)
    {
        this.sendOSC (address, Integer.valueOf (value), dump);
    }


    protected void sendOSC (final String address, final String value, final boolean dump)
    {
        this.sendOSC (address, (Object) StringUtils.fixASCII (value), dump);
    }


    protected void sendOSC (final String address, final Object value, final boolean dump)
    {
        this.sendOSC (address, address, value, value, dump);
    }


    /**
     * Tests if the vlaue(s) of given message is identical to that of the cache. If this is not the
     * case or if dump is true, the message is added to the messages list.The message will be sent
     * when flush gets called.
     *
     * @param cacheAddress The address under which to cache the message
     * @param address The address of the OSC message
     * @param testValue The value(s) to use for testing
     * @param value The value(s) of the OSC message
     * @param dump True to dump (ignore cache)
     */
    @SuppressWarnings("unchecked")
    protected void sendOSC (final String cacheAddress, final String address, final Object testValue, final Object value, final boolean dump)
    {
        if (!dump && compareValues (this.oldValues.get (cacheAddress), testValue))
            return;
        this.oldValues.put (cacheAddress, testValue);
        synchronized (this.messages)
        {
            final Object converted = convertBooleanToInt (value);
            this.messages.add (this.host.createOSCMessage (address, converted instanceof List ? (List<Object>) converted : Collections.singletonList (converted)));
        }
    }


    protected boolean isConnected ()
    {
        return this.oscClient != null;
    }


    /**
     * Convert the value to a list in case it is not already one. Also converts Boolean to Integer.
     *
     * @param value The value to convert
     * @return The converted value
     */
    @SuppressWarnings("unchecked")
    protected static List<Object> convertToList (final Object value)
    {
        if (value instanceof List)
            return List.class.cast (value);
        if (value instanceof Boolean)
            return Collections.singletonList (Integer.valueOf (((Boolean) value).booleanValue () ? 1 : 0));
        return Collections.singletonList (value);
    }


    /**
     * Compares two values. Additionally checks for list values.
     *
     * @param value1 The first value
     * @param value2 The second value
     * @return True if equal
     */
    protected static boolean compareValues (final Object value1, final Object value2)
    {
        if (value1 == null)
            return value2 == null;

        if (value1 instanceof List && value2 instanceof List)
        {
            final List<?> l1 = List.class.cast (value1);
            final List<?> l2 = List.class.cast (value2);
            final int size1 = l1.size ();
            final int size2 = l2.size ();
            if (size1 != size2)
                return false;
            for (int i = 0; i < size1; i++)
            {
                if (!l1.get (i).equals (l2.get (i)))
                    return false;
            }
            return true;
        }

        return value1.equals (value2);
    }


    private static Object convertBooleanToInt (final Object value)
    {
        return value instanceof Boolean ? Integer.valueOf (((Boolean) value).booleanValue () ? 1 : 0) : value;
    }


    protected void logMessages (final List<IOpenSoundControlMessage> messages)
    {
        if (!this.configuration.shouldLogOutputCommands () || messages.isEmpty ())
            return;

        final StringBuilder sb = new StringBuilder ();
        for (final IOpenSoundControlMessage message: messages)
        {
            final String address = message.getAddress ();
            if (this.configuration.filterHeartbeatMessages () && this.isHeartbeatMessage (address))
                continue;

            if (sb.length () > 0)
                sb.append ('\n');

            sb.append ("Sending: ").append (address).append (" [ ");
            final Object [] values = message.getValues ();
            for (int i = 0; i < values.length; i++)
            {
                if (i > 0)
                    sb.append (", ");
                sb.append (values[i]);
            }
            sb.append (" ]");
        }
        if (sb.length () > 0)
            this.model.getHost ().println (sb.toString ());
    }


    /**
     * Hook to ignore specific messages from logging.
     *
     * @param address The OSC address
     * @return Return true to ignore the message
     */
    protected boolean isHeartbeatMessage (final String address)
    {
        return false;
    }
}
