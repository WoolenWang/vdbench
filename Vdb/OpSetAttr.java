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

import java.io.*;


class OpSetAttr extends FwgThread
{
  private final static String c = "Copyright (c) 2010 Sun Microsystems, Inc. " +
                                  "All Rights Reserved. Use is subject to license terms.";


  public OpSetAttr(Task_num tn, FwgEntry fwg)
  {
    super(tn, fwg);
  }


  protected boolean doOperation()
  {
    while (!SlaveJvm.isWorkloadDone())
    {
      FileEntry fe = fwg.anchor.getFile(fwg.select_random);

      if (!fe.setBusy(true))
      {
        block(Blocked.FILE_BUSY);
        continue;
      }

      if (fe.isBadFile())
      {
        block(Blocked.BAD_FILE_SKIPPED);
        fe.setBusy(false);
        continue;
      }

      if (!fe.exists())
      {
        fe.setBusy(false);
        block(Blocked.FILE_MUST_EXIST);

        if (!canWeGetMoreFiles(msg))
          return false;

        continue;
      }

      /* We're finally happy: */
      long now = System.currentTimeMillis();
      long tod = Native.get_simple_tod();
      File file_ptr = new File(fe.getName());

      if (!file_ptr.setLastModified(now + YEAR))
      {
        common.failure("Unable to do a setattr request for file: " + fe.getName());
      }

      FwdStats.count(Operations.SETATTR, tod);
      fwg.blocked.count(Blocked.SET_ATTR);

      fe.setBusy(false);

      return true;
    }

    return false;
  }


  private String[] msg =
  {
    "Anchor: " + fwg.anchor.getAnchorName(),
    "Vdbench is trying to change attributes for a file, but there are no files",
    "available, and there are no threads currently active that can create new files."
  };
}
