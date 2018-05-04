package Vdb;

/*
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms of the Common
 * Development and Distribution License("CDDL") (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License at http://www.sun.com/cddl/cddl.html
 * or ../vdbench/license.txt. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice
 * in each file and include the License file at ../vdbench/licensev1.0.txt.
 *
 * If applicable, add the following below the License Header, with the
 * fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */


/*
 * Author: Henk Vandenbergh.
 */

import java.util.Vector;
import java.util.StringTokenizer;
import java.io.PrintWriter;
import Utils.Format;
import Utils.OS_cmd;


/**
 * This class handles calculation of cpu statistics for solaris, Windows and
 * Linux.
 */
public class CpuStats
{
  private final static String c = "Copyright (c) 2010 Sun Microsystems, Inc. " +
                                  "All Rights Reserved. Use is subject to license terms.";


  private static Kstat_cpu   old_cpu = new Kstat_cpu();
  private static Kstat_cpu   new_cpu = new Kstat_cpu();
  private static Kstat_cpu   dlt_cpu = new Kstat_cpu();

  private static int cpu_mhz   = 900;
  private static int cpu_count = -1;

  private static boolean cpu_reporting_active = false;



  /**
   * Get latest statistics, and calculate deltas between the last two intervals.
   */
  public static Kstat_cpu getNativeCpuStats()
  {
    /* Did we already have an error earlier? */
    if (dlt_cpu == null)
      return new Kstat_cpu();

    try
    {
      /* Get new statistics: */
      if (common.onLinux())
      {
        if ((new_cpu = Linux.getCpuStats()) == null)
          return  null;
      }

      else
      {

        long rc = Native.getCpuData(new_cpu);
        if (rc < 0)
        {
          if (common.onWindows())
          {
            common.ptod("Unable to obtain CPU statistics: rc=" + rc);
            common.ptod("Possible language issues for PDH field names.");
            common.ptod("Try running 'lodctr /R' to resolve this");
          }
          common.failure("Unable to obtain CPU statistics: rc=" + rc);
        }

        /* On solaris I get the combined number of ticks for all cpus. */
        /* This must be adjusted to microseconds total. */
        if (common.onSolaris())
        {
          new_cpu.cpu_total  = new_cpu.cpu_total  * new_cpu.cpu_hertz / new_cpu.cpu_count;
          new_cpu.cpu_idle   = new_cpu.cpu_idle   * new_cpu.cpu_hertz / new_cpu.cpu_count;
          new_cpu.cpu_user   = new_cpu.cpu_user   * new_cpu.cpu_hertz / new_cpu.cpu_count;
          new_cpu.cpu_kernel = new_cpu.cpu_kernel * new_cpu.cpu_hertz / new_cpu.cpu_count;
        }
      }

      /* Calculate delta (Solaris only): */
      if (common.onSolaris() || common.onLinux())
        dlt_cpu.cpu_delta(new_cpu, old_cpu);
      else
        dlt_cpu.cpu_copy(new_cpu);
      old_cpu.cpu_copy(new_cpu);

      return dlt_cpu;
    }

    /* We have a failure getting cpu stuff. Report and remember: */
    catch (UnsatisfiedLinkError e)
    {
      common.where(8);
      String txt = "UnsatisfiedLinkError while trying to collect CPU statistics. " +
                   "\n\t\tCPU reporting for this host will not be done.";
      SlaveJvm.sendMessageToConsole(txt);
      common.ptod(txt);
      common.ptod(e);
      dlt_cpu = null;
    }

    return new Kstat_cpu();
  }

  public static Kstat_cpu getDelta()
  {
    return dlt_cpu;
  }

  public static void setCpuReporting()
  {
    cpu_reporting_active = true;
  }
  public static boolean isCpuReporting()
  {
    return cpu_reporting_active;
  }


  private static int getProcessorCount()
  {
    if (cpu_count > 0)
      return cpu_count;

    if (!common.onSolaris())
    {
      cpu_count = Runtime.getRuntime().availableProcessors();
      return cpu_count;
    }

    cpu_count = getSolarisProcessorCount();
    return cpu_count;
  }

  private static int getSolarisProcessorCount()
  {
    int     tot_mhz  = 0;
    int     onlines  = 0;
    int     offlines = 0;
    boolean online   = false;

    try
    {
      OS_cmd ocmd = new OS_cmd();
      ocmd.setCmd("/usr/sbin/psrinfo -v ");
      ocmd.execute();

      /*
      psrinfo.addElement("Status of processor 0 as of: 01/12/2004 09:57:42       ");
      psrinfo.addElement("  Processor has been off-line since 12/12/2003 03:35:32.");
      psrinfo.addElement("  The sparcv9 processor operates at 900 MHz,           ");
      psrinfo.addElement("        and has a sparcv9 floating point processor.    ");
      psrinfo.addElement("Status of processor 1 as of: 01/12/2004 09:57:42       ");
      psrinfo.addElement("  Processor has been on-line since 12/12/2003 03:35:33.");
      psrinfo.addElement("  The sparcv9 processor operates at 900 MHz,           ");
      psrinfo.addElement("        and has a sparcv9 floating point processor.    ");

      psrinfo.addElement("Status of virtual processor 0 as of: 01/12/2004 09:35:08");
      psrinfo.addElement("   off-line since 01/09/2004 14:47:38.                   ");
      psrinfo.addElement("   The sparcv9 processor operates at 450 MHz,           ");
      psrinfo.addElement("         and has a sparcv9 floating point processor.    ");
      psrinfo.addElement("Status of virtual processor 1 as of: 01/12/2004 09:35:08");
      psrinfo.addElement("   on-line since 01/09/2004 14:47:39.                   ");
      psrinfo.addElement("   The sparcv9 processor operates at 450 MHz,           ");
      psrinfo.addElement("         and has a sparcv9 floating point processor.    ");
      psrinfo.addElement("Status of virtual processor 2 as of: 01/12/2004 09:35:08");
      psrinfo.addElement("   on-line since 01/09/2004 14:47:39.                   ");
      psrinfo.addElement("   The sparcv9 processor operates at 450 MHz,           ");
      psrinfo.addElement("         and has a sparcv9 floating point processor.    ");
      psrinfo.addElement("Status of virtual processor 3 as of: 01/12/2004 09:35:08");
      psrinfo.addElement("   on-line since 01/09/2004 14:47:39.                   ");
      psrinfo.addElement("   The sparcv9 processor operates at 450 MHz,           ");
      psrinfo.addElement("         and has a sparcv9 floating point processor.    ");
      */


      // Status of processor 0 as of: 04/05/2002 08:39:51
      //    Processor has been on-line since 03/08/2002 08:57:49.
      //    The sparcv9 processor operates at 336 MHz,
      //          and has a sparcv9 floating point processor.

      String[] psrinfo = ocmd.getStdout();
      for (int i = 0; i < psrinfo.length; i++)
      {
        String line = psrinfo[i];
        StringTokenizer st = new StringTokenizer(line);

        while (st.hasMoreElements())
        {
          String token = st.nextToken();
          //common.ptod("token: " + token);
          if (token.equals("on-line"))
            online = true;
          if (token.equals("off-line"))
            online = false;

          if (token.equals("operates"))
          {
            if (online)
            {
              onlines++;
              if (st.countTokens() > 1)
              {
                st.nextToken();
                tot_mhz += Integer.valueOf(st.nextToken()).intValue();
              }
            }
            else
              offlines++;
          }
        }
      }
    }

    catch (Exception e)
    {
    }


    if (onlines > 0)
    {
      cpu_mhz   = tot_mhz / onlines;
      common.ptod("Online processor count: " + onlines + " ; avg speed: " + cpu_mhz + " mhz");
      if (offlines > 0)
        common.ptod("Offline processor count: " + offlines);
    }

    else
    {
      onlines = Runtime.getRuntime().availableProcessors();
      common.ptod("Could not obtain processor count and speed. Using defaults:");
      common.ptod("Processor count: " + onlines + " ; avg speed: " + cpu_mhz + " mhz");
    }

    return onlines;
  }


  /**
   * Make sure that users have some indication that they are low on cycles
   */
  public static void cpu_shortage()
  {
    for (int i = 0; i < Host.getDefinedHosts().size(); i++)
    {
      Host host = (Host) Host.getDefinedHosts().elementAt(i);
      if (!host.anyWork())
        continue;

      Kstat_cpu kc = host.getSummaryReport().getData().getTotalCpuStats();
      if (kc == null)
        continue;

      double total = kc.user_pct() + kc.kernel_pct();
      if (total > 80)
      {
        common.ptod("*");
        common.ptod("host=" + host.getLabel());
        common.ptod(Format.f("* Warning: average processor utilization %.2f%% ", total));
        common.ptod("* Any processor utilization over 80% could mean that your system");
        common.ptod("* does not have enough cycles to run the highest rate possible ");
        common.ptod("*");

        common.psum("*");
        common.psum("host=" + host.getLabel());
        common.psum(Format.f("* Warning: average processor utilization %.2f%% ", total));
        common.psum("* Any processor utilization over 80% could mean that your system");
        common.psum("* does not have enough cycles to run the highest rate possible ");
        common.psum("*");
      }
    }
  }



  public static void main(String args[])
  {
    /*
    common.ptod("cpus: " + getProcessorCount());

    Native.kstatn_get_cpu(old_cpu);
    System.out.println("");
    System.out.println("cpu_count      " + old_cpu.cpu_count      );
    System.out.println("cpu_total      " + old_cpu.cpu_total      );
    System.out.println("cpu_idle       " + old_cpu.cpu_idle       );
    System.out.println("cpu_user       " + old_cpu.cpu_user       );
    System.out.println("cpu_kernel     " + old_cpu.cpu_kernel     );
    System.out.println("cpu_wait       " + old_cpu.cpu_wait       );
    System.out.println("usecs_per_tick " + old_cpu.usecs_per_tick );

    common.sleep_some(1000);

    Native.kstatn_get_cpu(new_cpu);
    System.out.println("");
    System.out.println("cpu_count      " + new_cpu.cpu_count      );
    System.out.println("cpu_total      " + new_cpu.cpu_total      );
    System.out.println("cpu_idle       " + new_cpu.cpu_idle       );
    System.out.println("cpu_user       " + new_cpu.cpu_user       );
    System.out.println("cpu_kernel     " + new_cpu.cpu_kernel     );
    System.out.println("cpu_wait       " + new_cpu.cpu_wait       );
    System.out.println("usecs_per_tick " + new_cpu.usecs_per_tick );

    dlt_cpu.cpu_delta(new_cpu, old_cpu);
    System.out.println("");
    System.out.println("cpu_count      " + dlt_cpu.cpu_count      );
    System.out.println("cpu_total      " + dlt_cpu.cpu_total      );
    System.out.println("cpu_idle       " + dlt_cpu.cpu_idle       );
    System.out.println("cpu_user       " + dlt_cpu.cpu_user       );
    System.out.println("cpu_kernel     " + dlt_cpu.cpu_kernel     );
    System.out.println("cpu_wait       " + dlt_cpu.cpu_wait       );
    System.out.println("usecs_per_tick " + dlt_cpu.usecs_per_tick );
    */

    getNativeCpuStats();

    common.sleep_some(1000);

    Kstat_cpu delta = getNativeCpuStats();
    System.out.println("");
    System.out.println("cpu_count      " + delta.cpu_count      );
    System.out.println("cpu_total      " + delta.cpu_total      );
    System.out.println("cpu_idle       " + delta.cpu_idle       );
    System.out.println("cpu_user       " + delta.cpu_user       );
    System.out.println("cpu_kernel     " + delta.cpu_kernel     );
    System.out.println("cpu_wait       " + delta.cpu_wait       );

    common.ptod("delta.user_pct():   " + delta.user_pct());
    common.ptod("delta.kernel_pct(): " + delta.kernel_pct());

  }
}


