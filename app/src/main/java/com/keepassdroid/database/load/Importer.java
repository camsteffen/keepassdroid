/*
 * Copyright 2009-2016 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database.load;

import com.keepassdroid.MyProgressTask;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDbHeaderV3;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.stream.LEDataInputStream;

import java.io.IOException;
import java.io.InputStream;

public abstract class Importer {

	public abstract PwDatabase openDatabase( InputStream inStream, String password, InputStream keyInputStream)
		throws IOException, InvalidDBException;

	public abstract PwDatabase openDatabase( InputStream inStream, String password, InputStream keyInputStream, MyProgressTask status )
		throws IOException, InvalidDBException;

	public static class Factory {

        public static Importer createImporter(InputStream is) throws IOException, InvalidDBException {
            int sig1 = LEDataInputStream.readInt(is);
            int sig2 = LEDataInputStream.readInt(is);

            if (PwDbHeaderV3.matchesHeader(sig1, sig2)) {
                return new ImporterV3();
            } else if (PwDbHeaderV4.matchesHeader(sig1, sig2)) {
                return new ImporterV4();
            } else {
                throw new InvalidDBException();
            }
        }
    }
}
