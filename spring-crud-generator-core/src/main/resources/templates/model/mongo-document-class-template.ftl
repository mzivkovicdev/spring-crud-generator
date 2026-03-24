<#if document?? && document>
@Document(collection = "${storageName}")
</#if><#t>
public class ${className} {

${fields}
${defaultConstructor}
${constructor}
${gettersAndSetters}
${hashCode}
${equals}
${toString}
}
