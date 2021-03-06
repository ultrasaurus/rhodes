#
#  syncengine_spec.rb
#  rhodes
#
#  Copyright (C) 2008 Rhomobile, Inc. All rights reserved.
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
require 'spec/spec_helper'
require 'rho/rho'

describe "SyncEngine" do
  
  it "should update syncserver at runtime" do
    SyncEngine.set_syncserver('http://example.com/sources/')
    @rho = Rho::RHO.new
    Rho::RhoConfig.syncserver.should == 'http://example.com/sources/'
  end
end
