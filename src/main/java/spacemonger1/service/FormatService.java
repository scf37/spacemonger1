package spacemonger1.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FormatService {

    public String getSizeString(Lang curLang, long size, long totalspace, boolean percent) {
        if (totalspace == 0) {
            totalspace = -1L; // (ui64)(-1) → all bits set → 0xFFFFFFFFFFFFFFFFL
        }

        if (percent) {
            // Compute size * 1000 / totalspace → gives X.X precision (e.g., 456 = 45.6%)
            long scaled = (size * 1000L) / totalspace;
            int value = (int) scaled; // safe: result ≤ 100000 in practice
            return String.format(curLang.percent_format, value / 10, value % 10);
        } else {
            int displayfull;
            int displayfractional;
            String displaytype;

            final long K = 1024L;
            final long M = K * 1024L;
            final long G = M * 1024L;

            if (size < K) {
                displayfull = (int) size;
                displayfractional = 0;
                displaytype = curLang.bytes;
            } else if (size < M) {
                displayfull = (int) (size / K);
                displayfractional = (int) (10 * (size % K) / K);
                displaytype = curLang.kb;
            } else if (size < G) {
                displayfull = (int) (size / M);
                displayfractional = (int) (10 * (size % M) / M);
                displaytype = curLang.mb;
            } else {
                displayfull = (int) (size / G);
                displayfractional = (int) (10 * (size % G) / G);
                displaytype = curLang.gb;
            }

            return String.format(curLang.size_format, displayfull, displayfractional, displaytype);
        }
    }

    public void printFileSize(Lang curLang, StringBuilder string, long size) {
        if (size == 0) {
            string.append("0 ").append(curLang.bytes);
            return;
        }

        char[] sizebuf = new char[256];
        int dest = 255; // index in buffer
        sizebuf[dest] = '\0'; // not used in Java, but kept for logic parity
        int ctr = 0;

        long s = size;
        while (s != 0) {
            ctr++;
            if (ctr == 4) {
                sizebuf[--dest] = curLang.digitcomma;
                ctr = 1;
            }
            sizebuf[--dest] = (char) ((s % 10) + '0');
            s /= 10;
        }

        // Append from `dest` to end of meaningful data
        string.append(sizebuf, dest, 256 - dest);
        string.append(' ').append(curLang.bytes);
    }


    public void printDate(Lang curLang, StringBuilder string, long filetime) {
        long milliseconds = filetime;

        LocalDateTime dt;
        try {
            dt = Instant.ofEpochMilli(milliseconds).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception e) {
            // Fallback to safe date if invalid
            dt = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        }

        int year = dt.getYear();
        int month = dt.getMonthValue(); // 1–12
        int day = dt.getDayOfMonth();
        int hour = dt.getHour();
        int minute = dt.getMinute();
        int second = dt.getSecond();

        // Clamp month (original does this)
        if (month < 1 || month > 12) month = 1;

        String monthName = curLang.monthnames[month - 1]; // safe due to clamp

        // Format: "%02d %s %04d   %d:%02d:%02d"
        String formatted = String.format("%02d %s %04d   %d:%02d:%02d",
                                         day, monthName, year, hour, minute, second);

        string.append(formatted);
    }

}
