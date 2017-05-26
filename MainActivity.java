package pedropablomoral.com.diferente;

import android.os.Bundle;
import android.widget.RelativeLayout;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            super.onCreateDrawer();

        RelativeLayout placeHolder = (RelativeLayout) findViewById(R.id.contenedor);
        getLayoutInflater().inflate(R.layout.activity_main, placeHolder);
    }
}
