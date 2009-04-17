package br.com.caelum.vraptor.vraptor2;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.caelum.vraptor.InterceptionException;
import br.com.caelum.vraptor.Interceptor;
import br.com.caelum.vraptor.core.InterceptorStack;
import br.com.caelum.vraptor.resource.ResourceMethod;
import br.com.caelum.vraptor.vraptor2.outject.Outjecter;

public class OutjectionInterceptor implements Interceptor{
    
    private static final String GET = "get";
    private static final String IS = "is";
    
    private static final Logger logger = LoggerFactory.getLogger(OutjectionInterceptor.class);
    private final HttpServletRequest request;
    private static final BeanHelper helper = new BeanHelper();
    private final Outjecter outjecter;
    
    public OutjectionInterceptor(HttpServletRequest request, Outjecter outjecter) {
        this.request = request;
        this.outjecter = outjecter;
    }

    public boolean accepts(ResourceMethod method) {
        return true;
    }

    public void intercept(InterceptorStack stack, ResourceMethod method, Object resourceInstance) throws IOException,
            InterceptionException {
        Class<?> type = method.getResource().getType();
        Method[] methods = type.getDeclaredMethods();
        for (Method outject : methods) {
            if(outject.getName().length()<3 || !(outject.getName().startsWith(IS) || outject.getName().startsWith(GET))) {
                continue;
            }
            if(outject.getParameterTypes().length!=0) {
                logger.error("A get method was found at " + type + " but was not used because it receives parameters. Fix it.");
                continue;
            } else if(outject.getReturnType().equals(void.class)) {
                logger.error("A get method was found at " + type + " but was not used because it returns void. Fix it.");
                continue;
            }
            try {
                Object result = outject.invoke(resourceInstance);
                String name = helper.nameForGetter(outject);
                logger.debug("Outjecting " + name);
                outjecter.include(name, result);
            } catch (IllegalArgumentException e) {
                throw new InterceptionException("Unable to outject value for " + outject.getName(), e);
            } catch (IllegalAccessException e) {
                throw new InterceptionException("Unable to outject value for " + outject.getName(), e);
            } catch (InvocationTargetException e) {
                throw new InterceptionException("Unable to outject value for " + outject.getName(), e.getCause());
            }
        }
        stack.next(method, resourceInstance);
    }

}
