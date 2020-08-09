package com.malconectado.jpautils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

import com.malconectado.exception.BadRequestParamException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * This processor transform a Map<String.String> in a jpa specification.
 * Based in Spring JPA
 * @author jcastillo
 *
 */
public final class QueryProcessor {

	/**
	 * Separator between name an operation.Example: name_lt, name is name variable, separator "_" and operation "lt"
	 */
	private String separator = "_";
	
	/** Used for multiple arguments functions like "between" and "in"  */
	private String paramSeparator = ",";
	
	/**  Less than operation: < */
	private String lessThanOP = "lt";
	
	/** Less equal operation: <= */
	private String lessEqualOP = "le";
	
	/** Greater than operation: > */
	private String greaterThanOP = "gt";
	
	/** Greater equal operation: >= */
	private String greaterEqualOP = "ge";
	
	/** Between operation, accept two parameters delimited by paramSeparator */
	private String betweenOP = "bt";
	
	/** IN operation, accept severals parameters delimited by paramSeparator */
	private String inOP = "in";

	/**Equal operation. Is Default*/
	private String equalOP = "=";
	
	/** Pageable size param */
	private String pageableSizeParam = "_size";
	
	/** Pageable page param */
	private String pageablePageParam = "_number";
	
	/** Pageable sort param */
	private String pageableSortParam = "_sort";
	
	/** Wildcard for additional pre process */
	private Consumer<Map<String, String>> preProcess;
		
	/**
	 * Transform a query in a specification
	 * @param query
	 * @return
	 */
	public <T> Specification<T> processQuery(Map<String, String> query) {
		
		/*final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
        	//final User u = (User) authentication.getPrincipal();
        	//System.out.println("query processor "  + u.getUsername());
        }*/

		if (preProcess!=null) {
			preProcess.accept(query);			
		}
		
		query.remove(pageableSizeParam);
		query.remove(pageablePageParam);
		query.remove(pageableSortParam);		
		
		
		Specification<T> ret = (i, cq, cb)-> 
			{			
				
				ArrayList<Predicate> predicates = new ArrayList<>();
				
				Expression<?> ex = null;
				
				//From<T, ?> from = null;
				
				//ArrayList<Object> paramList = new ArrayList<>();
				
				
				Class<?> targetType = null;
				String field = null;
				String value = null;
				
				for (Entry<String,String> entry : query.entrySet()) {
					String operation = equalOP;
					String key = entry.getKey();
					if (key.contains(separator)) {
						String[] s = key.split(separator);
						operation = s[1];
						key= s[0];
					}
					try {
						if (entry.getKey().contains(".")) {
							String[] param = key.split("\\.");
							String name = param[0];
							field = param[1];
							targetType = getType(i.getJavaType(), name);
							value = entry.getValue();
							Join<T, ?> join = i.join(param[0]);
							//from = join;
							ex = join.get(param[1]);					
						}else {
							ex = i.get(key);
							//from = i;
							field = key;
							value = entry.getValue();
							targetType = i.getJavaType();
						}						
						
						if (equalOP.equals(operation)) {
							predicates.add(cb.equal(ex,parseParam(targetType,field,value)));
						} else if (inOP.equals(operation)) {
							In<Object> inclause = cb.in(ex);
							for (String s : value.split(paramSeparator)) {
								inclause.value(parseParam(targetType,field,s));
							}
							predicates.add(inclause);
						} else if (lessThanOP.equals(operation)) {
							predicates.add(cb.lt((Expression<Number>)ex, (Number)parseParam(targetType,field,value)));	
						} else if (greaterThanOP.equals(operation)) {
							predicates.add(cb.gt((Expression<Number>)ex, (Number)parseParam(targetType,field,value)));	
						} else if (lessEqualOP.equals(operation)) {
							predicates.add(cb.le((Expression<Number>)ex, (Number)parseParam(targetType,field,value)));
						} else if (greaterEqualOP.equals(operation)) {
							predicates.add(cb.ge((Expression<Number>)ex, (Number)parseParam(targetType,field,value)));
						} else if (betweenOP.equals(operation)) {
							/*String[] vals = value.split(SEPARATOR);
							
							p.add(cb.between(
									(Expression<Number>)ex, 
									cb.literal((Number)parseParam(targetType,field,vals[0])),
									cb.literal((Number)parseParam(targetType,field,vals[1]))
									));
							
							p.add(cb.between(
									(Expression<Number>)ex, 
									(Number)parseParam(targetType,field,vals[0]),
									(Number)parseParam(targetType,field,vals[1])
									));*/
							break;	
						}
						
						
					} catch (SecurityException e) {
						throw new BadRequestParamException("", e);
					}
				}
				
				return cb.and(predicates.toArray(new Predicate[0]));
			};
		
		return ret;
	}
	
	/**
	 * Get value of a field of class based in method get definition
	 * @param clasz
	 * @param key
	 * @param value
	 * @return
	 */
	private static Object parseParam(Class<?> clasz,String key, String value) {
		if (key.startsWith("is")) {//boolean case
			return "true".equals(value);
		}else {
			try {
				Class<?> rt = getType(clasz, key);
				
				if (rt.isEnum()) {
					Class<? extends Enum> c = (Class<? extends Enum>) rt;
					return Enum.valueOf(c, value);
				}else if (rt == Long.class) {
					return Long.valueOf(value);
				}else if (rt == BigDecimal.class) {
					return new BigDecimal(value);
				}else if (rt == Integer.class) {
					return Integer.valueOf(value);
				}
			} catch ( SecurityException e) {
				e.printStackTrace();
			}
		}
		return value;
	}
	
	/**
	 * Get type of value of key, the method of class must be standard. For example:
	 * getName(), for manage name variable.
	 * @param clasz
	 * @param key
	 * @return
	 */
	private static Class<?> getType(Class<?> clasz,String key) {
		try {
			Method m = clasz.getMethod("get" + key.substring(0, 1).toUpperCase() + key.substring(1, key.length()));
			return m.getReturnType();
		} catch (NoSuchMethodException | SecurityException e) {
			throw new BadRequestParamException("inavlid.param");
		}
	}
	
	public Pageable processPageable(Map<String, String> query) {
		Integer number = 0;
		Integer size = Integer.MAX_VALUE;
		if (query.containsKey(pageablePageParam)) {
			number = Integer.valueOf(query.get(pageablePageParam));
		}
		
		if (query.containsKey(pageableSizeParam)) {
			size = Integer.valueOf(query.get(pageableSizeParam));
		}
		
		return PageRequest.of(number,size,Sort.by("id"));
	}

}
