package college.paul.john.puvroute;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

class Icon {
    private static Bitmap finish;
    private static Bitmap start;

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

    static Bitmap getFinish() {
        return finish;
    }

    static Bitmap getStart() {
        return start;
    }

}
