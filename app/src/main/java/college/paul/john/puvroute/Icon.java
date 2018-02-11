package college.paul.john.puvroute;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class Icon {
    private static Bitmap finish;
    private static Bitmap start;

    /*
        Warning this must be initialize before calling any function from this class.
        This will initially load the contents of the icons.
     */
    static void init(Context context) {
        int height = 100;
        int width = 100;
        BitmapDrawable bitmapdraw = (BitmapDrawable) context.getResources().getDrawable(R.drawable.finish);
        Bitmap b = bitmapdraw.getBitmap();
        finish = Bitmap.createScaledBitmap(b, width, height, false);

        bitmapdraw = (BitmapDrawable) context.getResources().getDrawable(R.drawable.start);
        b = bitmapdraw.getBitmap();
        start = Bitmap.createScaledBitmap(b, width, height, false);
    }

    /*
        Get the finish icon.
     */
    public static Bitmap getFinish() {
        return finish;
    }

    /*
        Get the start icon.
     */
    public static Bitmap getStart() {
        return start;
    }

}
