package brainwine.api.handlers;

import static brainwine.api.util.ContextUtils.*;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public class RwcPurchaseHandler implements Handler {
    
    @Override
    public void handle(Context ctx) throws Exception {
        error(ctx, "Sorry, RWC purchases are disabled.");
    }
}
