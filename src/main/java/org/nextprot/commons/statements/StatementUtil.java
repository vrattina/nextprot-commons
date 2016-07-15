package org.nextprot.commons.statements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.nextprot.commons.algo.MD5Algo;
import org.nextprot.commons.utils.StringUtils;

public class StatementUtil {

	public static void computeAndSetAnnotationIdsForRawStatements(List<RawStatement> statements) {
		
		
		HashMap<String, RawStatement> normalStatementsMap = new HashMap<String, RawStatement>();
		List<RawStatement> statementsOnModifiedSubjects = new ArrayList<RawStatement>();

		//Takes all normal statements and put them in a map and all the other put them in a list 
		for(RawStatement s : statements){
			if(!s.hasModifiedSubject()){
				s.computeAndSetAnnotationIds();
				normalStatementsMap.put(s.getStatementId(), s);
			}else {
				statementsOnModifiedSubjects.add(s);
			}
		}
		
		for(RawStatement complexStatement : statementsOnModifiedSubjects){
			
			String subjectIds = complexStatement.getSubjectStatementIds();
			setValues(complexStatement, StatementField.SUBJECT_ANNOT_ISO_IDS, subjectIds, StatementField.ANNOT_ISO_ID, normalStatementsMap);
			setValues(complexStatement, StatementField.SUBJECT_ANNOT_ENTRY_IDS, subjectIds, StatementField.ANNOT_ENTRY_ID, normalStatementsMap);

			String objectStatementsId = complexStatement.getObjectStatementId();
			setValues(complexStatement, StatementField.OBJECT_ANNOT_ISO_IDS, objectStatementsId, StatementField.ANNOT_ISO_ID, normalStatementsMap);
			setValues(complexStatement, StatementField.OBJECT_ANNOT_ENTRY_IDS, objectStatementsId, StatementField.ANNOT_ENTRY_ID, normalStatementsMap);
			
			//Compute annotation ids for this complex statement
			complexStatement.computeAndSetAnnotationIds();
		}
	}
	
	
	
	public static void setValues(
			RawStatement complexStatement, 
			StatementField fieldToSetInComplexStatement, 
			String referenceIds, 
			StatementField fieldTotakeFromSubject,
			Map<String, RawStatement> statementsDictionary) {
		
		Set<String> subjectIsoIds = new TreeSet<>();
		
		String[] referenceIdsArray = referenceIds.split(",");

		//Can be 1 or multiple subjects but most of the time it's just one
		for(String referenceId : referenceIdsArray){
			RawStatement referedStatement = statementsDictionary.get(referenceId); 
			
			if(referedStatement == null){
				throw new RuntimeException("Invalid statements. Can't find referenced statement " +  referenceId + ", referenced by statement " + complexStatement.getStatementId());
			}

			subjectIsoIds.add(referedStatement.getValue(fieldTotakeFromSubject));
		}

		// Setting subjects
		complexStatement.putValue(fieldToSetInComplexStatement, StringUtils.mkString(subjectIsoIds, "", ",", ""));
		
	}

}
