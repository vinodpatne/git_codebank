import org.springframework.web.servlet.HandlerInterceptor;

/**
*  The purpose of this classs is to add 'Access-Control-Allow-Origin' header to the response.
*
*  You need add this line to your spring application context to enable this interceptor - 
*  <mvc:interceptors>
*      <bean class="com.elm.mb.rest.interceptors.CORSInterceptor" />
*  </mvc:interceptors>
*  
**/

@Component
public class CORSInterceptor implements HandlerInterceptor {
   private static final Log LOG = LogFactory.getLog(CORSInterceptor.class);

   @Override
   public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

       LOG.trace("sending headers");
       response.setHeader("Access-Control-Allow-Origin", "*");
       response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE");
       response.setHeader("Access-Control-Max-Age", "3600");
       response.setHeader("Access-Control-Allow-Headers", "x-requested-with");

       return true;
   }

   @Override
   public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
        throws Exception {

   }

   @Override
   public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
        throws Exception {
        LOG.trace("afterCompletion is called");
   }

}
