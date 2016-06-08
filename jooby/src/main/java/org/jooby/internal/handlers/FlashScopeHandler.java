package org.jooby.internal.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jooby.Cookie;
import org.jooby.FlashScope;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.Route;

public class FlashScopeHandler implements Route.Filter {

  private String name;

  private Function<String, Map<String, String>> decoder;

  private Function<Map<String, String>, String> encoder;

  public FlashScopeHandler(final String cname, final Function<String, Map<String, String>> decoder,
      final Function<Map<String, String>, String> encoder) {
    this.name = cname;
    this.decoder = decoder;
    this.encoder = encoder;
  }

  @Override
  public void handle(final Request req, final Response rsp, final Route.Chain chain)
      throws Throwable {
    Optional<String> value = req.cookie(name).toOptional();
    Map<String, String> flashScope = value.map(decoder::apply)
        .orElseGet(HashMap::new);
    Map<String, String> copy = new HashMap<>(flashScope);

    req.set(FlashScope.NAME, flashScope);

    // wrap & proceed
    chain.next(req, new Response.Forwarding(rsp) {
      @Override
      public void send(final Result result) throws Throwable {
        // 1. no change detect
        if (flashScope.equals(copy)) {
          // 1.a. existing data available, discard
          if (flashScope.size() > 0) {
            rsp.cookie(new Cookie.Definition(name, "").maxAge(0));
          }
        } else {
          // 2. change detected
          if (flashScope.size() == 0) {
            // 2.a everything was removed from app logic
            rsp.cookie(new Cookie.Definition(name, "").maxAge(0));
          } else {
            // 2.b there is something to see in the next request
            rsp.cookie(name, encoder.apply(flashScope));
          }
        }
        // send
        super.send(result);
      }
    });

  }

}
