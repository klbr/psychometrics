/*
 * Copyright 2012 J. Patrick Meyer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itemanalysis.psychometrics.reliability;

import com.itemanalysis.psychometrics.data.VariableAttributes;
import com.itemanalysis.psychometrics.polycor.CovarianceMatrix;

import java.util.ArrayList;
import java.util.Formatter;


/**
 * Computes teh Feldt-Gilmer estimate of reliability. This method assumes score are
 * classically congeneric.
 */
public class FeldtGilmer extends AbstractScoreReliability{

	public FeldtGilmer(double[][] matrix){
		this.matrix=matrix;
        nItems = matrix.length;
	}

    public ScoreReliabilityType getType(){
        return ScoreReliabilityType.FELDT_GILMER;
    }
	
	private int getEll(){
        double[] offDiag= new double[nItems];
		for(int i=0;i<nItems;i++){
			offDiag[i]=this.rowSum(i)-matrix[i][i];
		}

		int maxIndex=0;
		double maxValue=offDiag[0];
		for(int i=1;i<nItems;i++){
			if(offDiag[i]>maxValue){
				maxIndex=i;
				maxValue=offDiag[i];
			}
		}
		return maxIndex;
	}
	
	private double[] D(int ell){
		double[] d = new double[nItems];
		double num=0.0;
		double denom=0.0;
		
		for(int i=0;i<nItems;i++){
			if(i==ell){
				d[i]=1.0;
			}else{
				num=this.rowSum(i)-matrix[i][ell]-matrix[i][i];
				denom=this.rowSum(ell)-matrix[i][ell]-matrix[ell][ell];
				d[i]=num/denom;
			}
		}
		return d;
	}

	private int getEllWithoutItemAt(int index){
		double[] offDiag= new double[nItems];
		for(int i=0;i<nItems;i++){
			if(i!=index) offDiag[i]=this.rowSum(i)-matrix[i][i];
		}

		int maxIndex=0;
		double maxValue=Double.MIN_VALUE;
		for(int i=0;i<nItems;i++){
			if(i!=index){
				if(offDiag[i]>maxValue){
					maxIndex=i;
					maxValue=offDiag[i];
				}
			}

		}
		return maxIndex;
	}

	private double[] DwithoutItemAt(int ell, int index){
		double[] d = new double[nItems];
		double num=0.0;
		double denom=0.0;
		double covAt = 0;

		for(int i=0;i<nItems;i++){
			if(i!=index){
				if(i==ell){
					d[i]=1.0;
				}else{
					covAt = matrix[i][ell];
					num=this.rowSum(i)-covAt-matrix[i][i];
					denom=this.rowSum(ell)-covAt-matrix[ell][ell];
					d[i]=num/denom;
				}
			}
		}
		return d;
	}
	
	public double value(){
		if(nItems<3) return Double.NaN;
		int ell = getEll();
		double[] d=D(ell);
		double sumD=0.0;
		double sumD2=0.0;
		double observedScoreVariance = this.totalVariance();
		double componentVariance = this.diagonalSum();
		
		for(int i=0;i<nItems;i++){
			sumD+=d[i];
			sumD2+=Math.pow(d[i], 2);
		}
		
		double fg=(Math.pow(sumD, 2)/(Math.pow(sumD, 2)-sumD2))*((observedScoreVariance-componentVariance)/observedScoreVariance);
		return fg;
	}

    /**
     * Computes reliability with each item omitted in turn. The first element in the array is the
     * reliability estimate without the first item. The second item in the array is the reliability
     * estimate without the second item and so on.
     *
     * @return array of item deleted estimates.
     */
    public double[] itemDeletedReliability(){
		double[] rel = new double[nItems];
		double totalVariance = this.totalVariance();
		double diagonalSum = this.diagonalSum();
		double totalVarianceAdjusted = 0;
		double diagonalSumAdjusted = 0;
		double reliabilityWithoutItem = 0;

		int ellAdj = 0;
		double[] dAdj=null;
		double sumDadj=0.0;
		double sumD2adj=0.0;
		double sumDadj2 = 0.0;

		for(int i=0;i<nItems;i++){
			//Compute item variance
			double itemVariance = matrix[i][i];

			//Compute sum of covariance between this item and all others
			double itemCovariance = 0;
			for(int j=0;j<nItems;j++){
				if(i!=j) itemCovariance += matrix[i][j];
			}
			itemCovariance *= 2;

			totalVarianceAdjusted = totalVariance - itemCovariance - itemVariance;
			diagonalSumAdjusted = diagonalSum - itemVariance;

			ellAdj = getEllWithoutItemAt(i);
			dAdj = DwithoutItemAt(ellAdj, i);
			for(int j=0;j<nItems;j++){
				if(i!=j) sumDadj+=dAdj[i];
				sumD2adj+=Math.pow(dAdj[i], 2);
			}

			sumDadj2 = Math.pow(sumDadj, 2);
			reliabilityWithoutItem = (sumDadj2/(sumDadj2-sumD2adj))*
					((totalVarianceAdjusted-diagonalSumAdjusted)/totalVarianceAdjusted);
			rel[i] = reliabilityWithoutItem;
		}
		return rel;
    }

    @Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		Formatter f = new Formatter(builder);
		String f2="%.2f";
		f.format("%15s", "Feldt-Gilmer = "); f.format(f2,this.value());
		return f.toString();
	}

    public String printItemDeletedSummary(ArrayList<VariableAttributes> var){
        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb);
        double[] del = itemDeletedReliability();
        f.format("%-56s", " Feldt-Gilmer  (SEM in Parentheses) if Item Deleted"); f.format("%n");
		f.format("%-56s", "========================================================"); f.format("%n");
        for(int i=0;i<del.length;i++){
            f.format("%-10s", var.get(i)); f.format("%5s", " ");
            f.format("%10.4f", del[i]); f.format("%5s", " ");f.format("%n");
        }
        return f.toString();
    }

}
