<#-- template include for network.ned.ftl -->
<#-- TODO: generateCoordinates -->
<#if parametricNED>
network ${ProjectName}
{
    parameters:
        int columns = default(${columns});
        int rows = default(${rows});
    submodules:
        node[columns*rows]: ${nodeTypeName};
    connections:
<#if grid>
        for i=0..rows-1, for j=0..columns-1 {
            node[i*columns+j].down <--> node[(i+1)*columns+j].up if i!=rows-1;
            node[i*columns+j].right <--> node[(i*columns+j)+1].left if j!=columns-1;
        }
<#elseif torus>
        for i=0..rows-1, for j=0..columns-1 {
            node[i*columns+j].down <--> node[((i+1)%rows)*columns+j].up;
            node[i*columns+j].right <--> node[(i*columns+(j+1)%columns].left;
        }
<#else>
   <#stop "one of torus and grid must be true">
</#if>        
}


<#else>
network ${ProjectName}
{
    submodules:
<#list 0..rows-1 as i>
  <#list 0..columns-1 as j>
        node_${i}_${j}: ${nodeTypeName};
  </#list>

</#list>
    connections:
<#if grid>
  <#list 0..rows-1 as i>
    <#list 0..columns-1 as j>
        <#if i!=rows-1   >node_${i}_${j}.down <--> node_${i+1}_${j}.up;</#if>
        <#if j!=columns-1>node_${i}_${j}.right <--> node_${i}_${j+1}.left;</#if>
    </#list>
  </#list>
<#elseif torus>
  <#list 0..rows-1 as i>
    <#list 0..columns-1 as j>
        node_${i}_${j}.down <--> node_${(i+1)%rows}_${j}.up;
        node_${i}_${j}.right <--> node_${i}_${(j+1)%columns}.left;
    </#list>
  </#list>
<#else>
   <#stop "one of torus and grid must be true">
</#if>        
}
</#if>

