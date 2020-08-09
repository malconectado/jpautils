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

	/**
	 * @return the separator
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * @param separator the separator to set
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * @return the paramSeparator
	 */
	public String getParamSeparator() {
		return paramSeparator;
	}

	/**
	 * @param paramSeparator the paramSeparator to set
	 */
	public void setParamSeparator(String paramSeparator) {
		this.paramSeparator = paramSeparator;
	}

	/**
	 * @return the lessThanOP
	 */
	public String getLessThanOP() {
		return lessThanOP;
	}

	/**
	 * @param lessThanOP the lessThanOP to set
	 */
	public void setLessThanOP(String lessThanOP) {
		this.lessThanOP = lessThanOP;
	}

	/**
	 * @return the lessEqualOP
	 */
	public String getLessEqualOP() {
		return lessEqualOP;
	}

	/**
	 * @param lessEqualOP the lessEqualOP to set
	 */
	public void setLessEqualOP(String lessEqualOP) {
		this.lessEqualOP = lessEqualOP;
	}

	/**
	 * @return the greaterThanOP
	 */
	public String getGreaterThanOP() {
		return greaterThanOP;
	}

	/**
	 * @param greaterThanOP the greaterThanOP to set
	 */
	public void setGreaterThanOP(String greaterThanOP) {
		this.greaterThanOP = greaterThanOP;
	}

	/**
	 * @return the greaterEqualOP
	 */
	public String getGreaterEqualOP() {
		return greaterEqualOP;
	}

	/**
	 * @param greaterEqualOP the greaterEqualOP to set
	 */
	public void setGreaterEqualOP(String greaterEqualOP) {
		this.greaterEqualOP = greaterEqualOP;
	}

	/**
	 * @return the betweenOP
	 */
	public String getBetweenOP() {
		return betweenOP;
	}

	/**
	 * @param betweenOP the betweenOP to set
	 */
	public void setBetweenOP(String betweenOP) {
		this.betweenOP = betweenOP;
	}

	/**
	 * @return the inOP
	 */
	public String getInOP() {
		return inOP;
	}

	/**
	 * @param inOP the inOP to set
	 */
	public void setInOP(String inOP) {
		this.inOP = inOP;
	}

	/**
	 * @return the equalOP
	 */
	public String getEqualOP() {
		return equalOP;
	}

	/**
	 * @param equalOP the equalOP to set
	 */
	public void setEqualOP(String equalOP) {
		this.equalOP = equalOP;
	}

	/**
	 * @return the pageableSizeParam
	 */
	public String getPageableSizeParam() {
		return pageableSizeParam;
	}

	/**
	 * @param pageableSizeParam the pageableSizeParam to set
	 */
	public void setPageableSizeParam(String pageableSizeParam) {
		this.pageableSizeParam = pageableSizeParam;
	}

	/**
	 * @return the pageablePageParam
	 */
	public String getPageablePageParam() {
		return pageablePageParam;
	}

	/**
	 * @param pageablePageParam the pageablePageParam to set
	 */
	public void setPageablePageParam(String pageablePageParam) {
		this.pageablePageParam = pageablePageParam;
	}

	/**
	 * @return the pageableSortParam
	 */
	public String getPageableSortParam() {
		return pageableSortParam;
	}

	/**
	 * @param pageableSortParam the pageableSortParam to set
	 */
	public void setPageableSortParam(String pageableSortParam) {
		this.pageableSortParam = pageableSortParam;
	}

	/**
	 * @return the preProcess
	 */
	public Consumer<Map<String, String>> getPreProcess() {
		return preProcess;
	}

	/**
	 * @param preProcess the preProcess to set
	 */
	public void setPreProcess(Consumer<Map<String, String>> preProcess) {
		this.preProcess = preProcess;
	}
	
	

}
