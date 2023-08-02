package org.aujee.sundew.processor.support.autoconfiggroup;

import org.aujee.sundew.processor.ProcLogger;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

class DataExtractor {

    DataExtractor() {
    }

    Map<String, Map.Entry<Boolean, List<String[]>>> getData(
            Set<? extends Element> properElements,
            Function<VariableElement, String> fileNameExtractor,
            Predicate<VariableElement> branchNamingSplitter,
            Function<VariableElement, String> customBranchExtractor,
            Elements elementUtils) {

        return extractFinal(extractByUserInput(properElements, fileNameExtractor, branchNamingSplitter),
                customBranchExtractor, elementUtils);
    }

    private Map<String, Map<Boolean, List<VariableElement>>> extractByUserInput(
            Set<? extends Element> properElements,
            Function<VariableElement, String> fileNameExtractor,
            Predicate<VariableElement> branchNamingSplitter) {

        return properElements.stream()
                .map(element -> (VariableElement) element)
                .collect(Collectors.groupingBy(
                        fileNameExtractor,
                        () -> HashMap.newHashMap(properElements.size()),
                        Collectors.partitioningBy(
                                branchNamingSplitter
                        )));
    }

    private Map<String, Map.Entry<Boolean, List<String[]>>> extractFinal(
            Map<String, Map<Boolean, List<VariableElement>>> byUserInput,
            Function<VariableElement, String> customBranchExtractor,
            Elements elementUtils) {

        Map<String, Map.Entry<Boolean, List<String[]>>> elementDataContainer = HashMap.newHashMap(byUserInput.size());

        for (Map.Entry<String, Map<Boolean, List<VariableElement>>> perFileName : byUserInput.entrySet()) {
            String fileName = perFileName.getKey();
            Set<Map.Entry<Boolean, List<VariableElement>>> perBranchNaming = perFileName.getValue().entrySet();

            //User mixing branch naming in one file, mistake check.
            //(There is no possibility that both lists are empty. After ElementsSelector only proper elements are processed.)
            if (perBranchNaming.stream().allMatch((Predicate.not(e -> e.getValue().isEmpty())))) {
                ProcLogger.branchNamesMixMes(fileName);
                continue;
            }

            for (Map.Entry<Boolean, List<VariableElement>> next : perBranchNaming) {
                //We skipped processing file with mixed branch naming so one List will be empty.
                List<VariableElement> values = next.getValue();
                if (values.isEmpty()) {
                    continue;
                }
                List<VariableElement> sortedValues;
                boolean defaults;
                if (next.getKey()) {
                    defaults = true;
                    sortedValues = sortByDefault(values, elementUtils);
                    List<String[]> dataStrings = extractElementData(sortedValues, defaults, null);
                    elementDataContainer.put(fileName, Map.entry(defaults, dataStrings));
                } else {
                    defaults = false;
                    sortedValues = sortByCustom(values, customBranchExtractor);
                    List<String[]> dataStrings = extractElementData(sortedValues, defaults, customBranchExtractor);
                    elementDataContainer.put(fileName, Map.entry(defaults, dataStrings));
                }
            }
        }
        return elementDataContainer;
    }

    private List<String[]> extractElementData(List<VariableElement> elements,
                                              boolean withDefaults,
                                              Function<VariableElement, String> customBranchExtractor) {
        return elements.stream()
                .map(element -> {
                    String[] dataArray = new String[4];
                    dataArray[0] = element.getEnclosingElement().asType().toString();//enclosing class canonical name
                    dataArray[1] = element.getSimpleName().toString();//variable name
                    String type = element.asType().toString();//variable type
                    int optionalLastDot = type.lastIndexOf(".");
                    String simpleType = optionalLastDot == -1 ? type : type.substring(optionalLastDot + 1);
                    dataArray[2] = simpleType;
                    if (withDefaults) {
                        dataArray[3] = null;
                    } else {
                        dataArray[3] = customBranchExtractor.apply(element);
                    }
                    return dataArray;
                })
                .toList();
    }

    private List<VariableElement> sortByDefault(List<VariableElement> elements, Elements elementUtils) {
        return elements.stream()
                .sorted(comparing(e -> elementUtils.getPackageOf((VariableElement) e).getQualifiedName().toString())
                        .thenComparing((e1, e2) -> {
                            String s1 = ((VariableElement) e1).getEnclosingElement().asType().toString();
                            String s2 = ((VariableElement) e2).getEnclosingElement().asType().toString();
                            return s1.compareTo(s2);
                        })
                        .thenComparing((e1, e2) -> {
                            String s1 = ((VariableElement) e1).getSimpleName().toString();
                            String s2 = ((VariableElement) e2).getSimpleName().toString();
                            return s1.compareTo(s2);
                        }))
                .toList();
    }

    private List<VariableElement> sortByCustom(List<VariableElement> elements,
                                               Function<VariableElement, String> customBranchExtractor) {
        return elements.stream()
                .sorted(comparing(customBranchExtractor))
                .toList();
    }
}
