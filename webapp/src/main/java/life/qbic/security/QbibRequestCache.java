package life.qbic.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class QbibRequestCache implements RequestCache {

  @Override
  public void saveRequest(HttpServletRequest request, HttpServletResponse response) {

  }

  @Override
  public SavedRequest getRequest(HttpServletRequest request, HttpServletResponse response) {
    return null;
  }

  @Override
  public HttpServletRequest getMatchingRequest(HttpServletRequest request,
      HttpServletResponse response) {
    return null;
  }

  @Override
  public void removeRequest(HttpServletRequest request, HttpServletResponse response) {

  }
}
