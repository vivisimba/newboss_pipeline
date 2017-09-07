import java.text.SimpleDateFormat

//节点中的工作目录，与jenkins系统配置中的pod模板中的working directory一致
def workDirectory = "/home/jenkins/"
//基础镜像当中的第三方公共jar包目录
def thirjarsDirInBaseImage = "/public-boss-jarfiles"
//构建服务器中的第三方公共jar包目录（遍历，获得列表）
def thirjarsDirInBuildImage = "/home/public-boss-jarfiles"
def jobName = env.JOB_NAME
def buildnum = env.BUILD_NUMBER
//保存文件使用
//def version = env.VERSION
//def builder = env.BUILDER
def branch = env.BRANCH
//保存文件使用
def tag = env.IMAGETAG
def tasks = [:]
//def nowdateFormat = new SimpleDateFormat("yyMMdd")
//def nowDateStr = nowdateFormat.format(new Date())
//def innerTag
//if (tag) {
//	innerTag = "${tag}"
//}
//else {
//	innerTag = "${version}-${buildnum}-${nowDateStr}"
//}
def allList = [
    "admin-billing-ui",
    "admin-crm-ui",
    "admin-oss-ui",
    "admin-product-ui",
    "admin-public-ui",
	"account-center-service",
	"account-service",
	"agent-web",
	"api-gateway-service",
	"area-service",
	"billing",
	"callcenter-proxy",
	"card-center-service",
	"card-service",
	"channel-service",
	"check-service",
	"collection-center-service",
	"collection-service",
	"customer-center-service",
	"customer-service",
	"customer-ui",
	"haiwai-proxy",
	"iom-center-service",
	"iom-service",
	"job-service",
	"jobserver",
	"knowledge-service",
	"knowledge-ui",
	"message-center-service",
	"note-center-service",
	"note-service",
	"operator-service",
	"operator-ui",
	"order-center-service",
	"order-job",
	"order-service",
	"partner-service",
    "partner-ui",
	"platform-cache-config",
	"platform-config",
	"pms-center-service",
	"pms-frontend-conax-service",
	"pms-frontend-ott-service",
	"pms-partition-service",
    "portal-ui",
	"problem-center-service",
	"problem-service",
	"product-service",
	"resource-center-service",
	"resource-service",
    "resource-ui",
	"starDA-web",
	"system-service",
    "worker-ui",
    ]


def moduleStr = env.moduleStr
def moduleList = []

if (moduleStr.contains("buildall")) {
	moduleList = allList.clone()
}
else {
	moduleList = moduleStr.tokenize(',')
}

if(!moduleList) {
	echo "There has been no changes in any module since last successful building."
}
else {
	
	allList.each{moduleName->
	//moduleList.each{moduleName->
		
		tasks[moduleName] = {
			def codeDir = "${workDirectory}workspace/${jobName}/${moduleName}"
	
			node('jnlp'){
				if(moduleName in moduleList) {
					
					
					/*
					* ==============
					* 节点中获取代码
					* ==============
					*/
					stage('getcode'){
						sh """
							git clone -b ${branch} http://jenkins:startimes@10.0.250.70:8088/boss/stariboss.git ${moduleName}
							if [ ${moduleName} = "agent-web" ]
							then
								cp -r -i /home/ci-python/agent-web-node_modules ${codeDir}/agent-web/src/main/client/node_modules
							fi
							if [ ${moduleName} = "operator-app" ]
							then
								cp -r -i /home/ci-python/operator-app-node_modules ${codeDir}/operator-app/src/main/client/node_modules
							fi
							"""
					}
				
					
					/*
					* ==============
					* 节点当中构建对应模块
					* ==============
					*/
					stage('build'){
						sh """
							chmod 777 /home/ci-python/build.py
							python /home/ci-python/build.py ${codeDir} ${moduleName}
							cd ./${moduleName}
							. /etc/profile
							. ./gradleTaskList.config
							./gradlew \$GRADLETASK
							"""
					}
								
								
					/*
					* ==============
					* 将构建产物和模块对应的dockerfile移动到代码根目录下的outputFileFolder文件夹中
					* ==============
					*/
					stage('move building output to dir: outputFileFolder'){
						sh """
							chmod 777 /home/ci-python/mvFileToOutputFileFolder.py
							python /home/ci-python/mvFileToOutputFileFolder.py ${codeDir} ${moduleName}
							"""
					}
								
								
					/*
					* ==============
					* 删除公共jar包，并构建镜像
					* ==============
					*/
					stage('modify third jars and build images'){
						sh """
							chmod 777 /home/ci-python/buildDockerImage.py
							python /home/ci-python/buildDockerImage.py ${codeDir} ${moduleName} ${tag}
							"""
				//					sleep(3000)
					}
								
								
					/*
					 * ==============
					 * 将构建产物上传ftp中
					 * ==============
					 */
					//			stage('upload building product to ftp'){
					//				sh "echo ${codeDir}"
					//				sh "echo '============='"
					//				sh """
//                      chmod 777 /home/ci-python/build.py
//                      python /home/ci-python/build.py ${codeDir} ${moduleName}
//                """
					//				sleep(3000)
					//			}
					
					
				}
				else {
					// 获取上次镜像，打上新的标签
				}
				
				
			}
			
		}
	
	}
	
	//并行执行各节点任务
	parallel tasks
	
}

