/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.datasave;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.processing.AbstractProcessProvider;
import org.sensorhub.process.datasave.config.DatasaveProcessConfig;

public class ProcessDescriptors extends AbstractProcessProvider
{
    
    public ProcessDescriptors()
    {
        addImpl(DatasaveProcess.INFO);
    }

    @Override
    public String getModuleName() {
        return "Data Saving Process";
    }

    @Override
    public String getModuleDescription()
    {
        return "Processing module configured for storing data from an OSH database";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return DatasaveProcessModule.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return DatasaveProcessConfig.class;
    }

}
