package semiLA.layoutDesigner.analysis.Micro;

//import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Date;
import java.sql.Timestamp;

import semiLA.data.device.EQ;

//import semiLA.layoutDesigner.TestData;
import semiLA.io.file.LogFile;
import semiLA.layoutDesigner.CDataSet;
import semiLA.layoutDesigner.CResultSet;
import semiLA.layoutDesigner.Area.CBay;
import semiLA.layoutDesigner.Area.CZone;
//import semiLA.layoutDesigner.EQ.CEQ;
import semiLA.layoutDesigner.EQ.CModule;
import semiLA.layoutDesigner.EQ.CRoom;
import semiLA.layoutDesigner.EQ.CSubRoom;

public class MicroGenetic
{
	CDataSet dataSet = CDataSet.getInstance();
	CResultSet resultSet = CResultSet.getInstance();

	/** Room , ���� ���� �ǹ� */
	private CRoom room;
	int population_number = 100;
	int max_iteration = 2;
	
	public int accumulated_value = 0;
	public double current_best_fitness = 1000000;
	
//	GA�� ��� ���� �� % �̳��� ���ɰ����� ������ �׸� ������
	int stationary_number = 4;
	double stationary_slack = 0.001;
	
//	���� ������ ����� 11���� fix�ߴµ� �ٸ������� �޾ƿ��½����� ������ �ʿ������
	int facil_number =11;
	
	private ArrayList<Renewal_MicroOrganism> n_lstPopulation = new ArrayList<Renewal_MicroOrganism>();
	private ArrayList<Double> lst_roulette;


	
	
	/** Module�� Integer�� Mapping */
	private Map<Integer, CModule> propertiesByInt;
	private Map<String, Integer> propertiesByName;

	/** Module�� �����ϴ� ����Ʈ */
	private ArrayList<CModule> moduleList;

	/** �α�(Population, ����ü�� ����)�� ��Ÿ���� ���� */
	private ArrayList<MicroOrganism> lstPopulations;

	/** ������ �˰��� ��꿡�� ����ϴ� �α�(Population, ����ü�� ����) ������ ���� */
	private ArrayList<MicroOrganism> buffer;

	/** ���� fitness function ��*/
	private double pastFitnessFunctionValue;
	private double currentFitnessFunctionValue;
	private boolean isBetterSolution;
	
	/** ��⺰ ���� �� */
	private int[] moduleEqNum;

	
	
	/**
	 * ������
	 */
	public MicroGenetic()
	{
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 13 ���� 9:54:34
	 * @param logFile 
	 * @date : 2010. 07. 13
	 * @�����̷� :
	 * @Method ���� : GA�� Run �ϴ� Method
	 */
	
	
	public Renewal_MicroOrganism start()
	{
		System.out.println("GA ����");
		gen_population();		
		
		calfitness_and_set(n_lstPopulation);

		
//		�ϴ��� ���� ������ �ϳ� �־����
		Renewal_MicroOrganism current_solution = new Renewal_MicroOrganism();
		
		int accumulated_value = 0;
		
		for(int i=0; i<max_iteration; i++) {
			System.out.println(i+"generation ����");
			// ���� generation�� fitness ���
			System.out.println(i+" generation�� fitness"+elitism().fitness_value);
			
			ArrayList<Renewal_MicroOrganism> temp_population = new ArrayList<Renewal_MicroOrganism>();
//			���� ������ �ϳ��� �ϴ� �߰�
			temp_population.add(elitism());
			
			while(temp_population.size() < population_number) {
				
//				�ߺ��Ǵ� �ֵ��� ���� ���̴°� ������ �ߺ� ���� �ڵ嵵 �߰��ؾ��մϴ�
				Renewal_MicroOrganism selected_one = selection();
				Renewal_MicroOrganism selected_two = selection();
				
				Renewal_MicroOrganism[] temp_result = crossover(selected_one,selected_two);
//				
//				System.out.println("�ٲ���");
//				for (i = 0; i < selected_one.each_chromosome.size(); i++)
//				{
//				System.out.print(selected_one.each_chromosome.get(i).getM_strName()+" ");
//				}
//				System.out.println("");
//				System.out.println("�ѳ� Ȯ��");
//				for (i = 0; i < temp_result[0].each_chromosome.size(); i++)
//				{
//				System.out.print(temp_result[0].each_chromosome.get(i).getM_strName()+" ");
//				}
//				System.out.println("");
//				for (i = 0; i < temp_result[1].each_chromosome.size(); i++)
//				{
//				System.out.print(temp_result[1].each_chromosome.get(i).getM_strName()+" ");
//				}
				
				temp_population.add(temp_result[0]);
				temp_population.add(temp_result[1]);
			}
			
			
			//System.out.println("�� �������� Ȯ���غ���");
			temp_population = mutation(temp_population);
			
//			------------------------Fitness �ϴ� ����ϴ°� �ʿ��ϴ�------------------------
//			temp_population = cal_fitness(temp_population);
			
			Date date3 =new Date();
			System.out.println(new Timestamp(date3.getTime()));
			
			calfitness_and_set(temp_population);
			
			Date date4 =new Date();
			System.out.println(new Timestamp(date4.getTime()));
			
			
//			cast�ϴ°� �´����� �𸣰���
			n_lstPopulation = (ArrayList<Renewal_MicroOrganism>) temp_population.clone();
			
//			������ ã���� ������ break		
			if(check_stop_criteria(elitism())) {
				//break;
			}
		}
		
		return elitism();
	}
	
	
//	���絵 ������ üũ�ϴ� �Լ�
	public boolean check_stop_criteria(Renewal_MicroOrganism check_this)
	{
		if(check_this.fitness_value < current_best_fitness * (1 + stationary_slack) && accumulated_value > stationary_number) {
	
			return true;	
		}else if(check_this.fitness_value < current_best_fitness * (1 + stationary_slack)){
			current_best_fitness = check_this.fitness_value;
			accumulated_value+=1;
			return false;	
		}else {
			accumulated_value = 0;
			return false;	
		}
	}
	
	
//	���� ���� �� �ϳ��� ������ �ٴϱ�
	public Renewal_MicroOrganism elitism(){
		
		double best_solution = 1000000000000000000000000000000000.00;
		int best_index = -1;
//		fitness�� ū�� ������ ������ �������� ���� �޶������ ������ �켱�� ������ ���ٰ� ��
		for(int i=0;i<n_lstPopulation.size();i++) {
			if(n_lstPopulation.get(i).fitness_value < best_solution) {
				best_solution = n_lstPopulation.get(i).fitness_value;
				best_index = i;
			}
		}
		return n_lstPopulation.get(best_index);
	}
	
	public void calfitness_and_set(ArrayList<Renewal_MicroOrganism> input_array){
//		fitness ����ϰ� ��ġ�� ����� �ݿ��� ũ�θ����� ����
		for(int chromosome_index=0;chromosome_index<input_array.size() ; chromosome_index++) {
			input_array.set(chromosome_index, eqArrange(input_array.get(chromosome_index)));
			calcFitness_renew(input_array.get(chromosome_index));
			//System.out.println(input_array.get(chromosome_index).fitness_value);
		}
		
		//������ �κ� �־ ������ ��
//		convertEQCoord(population);
//		convertEQCoord();
//		convertFixedEqCoord();
//		convertZoneCoord();
//		
//		
//		convertEQCoord(population);
////		convertEQCoord();
//		convertZoneCoord();
//		convertFixedEqCoord();
		
		
	}
//	�ʱ� population�� �����ϴ� �ڵ�
	public void gen_population()
	{
		for(int i = 0; i < population_number ; i++) {
			
			Renewal_MicroOrganism individual_chromosome = new Renewal_MicroOrganism();
			
			ArrayList<CModule> temp_list = (ArrayList<CModule>) resultSet.direct_PRC_order.clone();
			int start_ind = 0;
			for(int num_prc : resultSet.direct_PRC_number) {
				int to_ind = start_ind + num_prc;
				Collections.shuffle(temp_list.subList(start_ind, to_ind)); 
				start_ind = to_ind;
			}		
			individual_chromosome.set_each_chromosome(temp_list);
			
			n_lstPopulation.add(individual_chromosome);
		}
	}
	
	
//	Crossover�� ũ�θ��� �ϳ��� selection�ϴ� �ڵ�
	public Renewal_MicroOrganism selection()
	{
		make_lst_roulette();
		
		Random random = new Random();
		
		double select_probability = random.nextDouble() * lst_roulette.get(lst_roulette.size()-1);
		
		boolean stop_value = false;
		
		int select_index = 0;
		int selected_index = -1;
		double prev_number = 0;
		for(double each_roul : lst_roulette) {
			if(prev_number < select_probability && select_probability <= each_roul) {
				selected_index = select_index;
				stop_value = true;
			}
			if(stop_value) {
				break;
			}
			select_index++;
			prev_number = each_roul;
		}

		return n_lstPopulation.get(selected_index);
	}
	

	
//	�귿 �� ����� ���� ���� Ȯ��list �����(��Ȯ�� Ȯ���� �ƴ�,������ ���Ұ� 1�� ������ ����)
	public void make_lst_roulette() {
		lst_roulette = new ArrayList<Double>();
		
//		�� �귿������ �������� ��� list ����(lst_roulette) 
		double prev_value = 0.0;
		for(Renewal_MicroOrganism popul : n_lstPopulation) {
			double next_value = prev_value + popul.roulette_probability;
			lst_roulette.add(next_value);
			prev_value += popul.roulette_probability;
		}
	}
	
	
//	2���� microorganism�� ��ǲ�޾� [�ڼ� microorganism,�ڼ� microorganism]���� return���ִ� �Լ�
	public Renewal_MicroOrganism[] crossover(Renewal_MicroOrganism parent1,Renewal_MicroOrganism parent2 )
	{
		Random random = new Random();
		
//		0(����) ~ n-1 ������ ���� ����. ������ 11�� �ϵ��ڵ� �ߴµ� resultSet.getM_lstRoomOrder() �� �۵��ϸ� �̰ɷ� �ٲٴ��� �ؾ���
		int changer_index =  random.nextInt(facil_number-2)+1;
		
		
		int prc_index =0;
		for(int i=0;i<changer_index;i++){
			prc_index += resultSet.direct_PRC_number.get(i);
		}
		//System.out.println(prc_index);
		
		Renewal_MicroOrganism temp_descendent1 = new Renewal_MicroOrganism();
		Renewal_MicroOrganism temp_descendent2 = new Renewal_MicroOrganism();
		
		temp_descendent1.each_chromosome.addAll(parent1.each_chromosome.subList(0, prc_index));
		temp_descendent1.each_chromosome.addAll(parent2.each_chromosome.subList(prc_index,parent2.each_chromosome.size()));
		
		temp_descendent2.each_chromosome.addAll(parent2.each_chromosome.subList(0, prc_index));
		temp_descendent2.each_chromosome.addAll(parent1.each_chromosome.subList(prc_index,parent2.each_chromosome.size()));
		
		Renewal_MicroOrganism[] re_array = {temp_descendent1, temp_descendent2};
		return re_array;
	}
	
	
//	Mutation�ϴ� �ڵ�
	public ArrayList<Renewal_MicroOrganism> mutation(ArrayList<Renewal_MicroOrganism> population)
	{
		    Random rand = new Random();
		    
//		    ���� Ȯ��
		    double selected_probaility = 0.01;

//    		arraylist�� ��ȸ�ϸ鼭 Ȯ������ ���� muatation����� �ɼ��� �ȵɼ���
		    for(int i=0;i<population.size();i++) {
		    	if(rand.nextDouble() < selected_probaility) {
		    		
					//System.out.println("");
					for (int k = 0; k < population.get(i).each_chromosome.size(); k++)
					{
					//System.out.print(population.get(i).each_chromosome.get(k).getM_strName()+" ");
					}
					//System.out.println("");
		    		
		    		int facil_slect =  rand.nextInt(facil_number-1);
		    		
		    		int prc_index = 0;
		    		int prc_num = resultSet.direct_PRC_number.get(0);
		    		
		    		for(int j=0;j < facil_slect;j++){
//		    			prc_index += resultSet.direct_PRC_number.get(j);
		    			prc_index += prc_num;
		    			prc_num =  resultSet.direct_PRC_number.get(j+1);
		    		}
		    		
		    		int first_selected_prc = rand.nextInt(prc_num-1)+prc_index;
		    		CModule move_1 = population.get(i).each_chromosome.get(first_selected_prc);
		    		int second_selected_prc;
		    		CModule move_2;
		    		
//		    		Ȥ�� ���� index�� ���� �� �����ϱ�
		    		do{
		    			second_selected_prc = rand.nextInt(prc_num-1)+prc_index;
		    			move_2 = population.get(i).each_chromosome.get(second_selected_prc);
		    		} while(first_selected_prc == second_selected_prc); 
		    		
		    		//System.out.println("�����ε���");
		    		//System.out.println(first_selected_prc);
		    		//System.out.println(second_selected_prc);
		    		
		    		population.get(i).each_chromosome.set(first_selected_prc,move_2);
		    		population.get(i).each_chromosome.set(second_selected_prc,move_1);
//					System.out.println("�ٲ����");
//					for (i = 0; i < selected_one.each_chromosome.size(); i++)
//					{
//					System.out.print(selected_one.each_chromosome.get(i).getM_strName()+" ");
//					}				
					//System.out.println("");
					for (int k = 0; k < population.get(i).each_chromosome.size(); k++)
					{
					//System.out.print(population.get(i).each_chromosome.get(k).getM_strName()+" ");
					}
					//System.out.println("");
		    		
		    		
		    	}
			}
		    
		    
		    return population;
	}
	
//	population�� ���� ���� ũ�θ����� return���ִ� �Լ�
	public Renewal_MicroOrganism find_best_chromosome(ArrayList<Renewal_MicroOrganism> population){
		
		double best_solution = 10000000000.00;
		int best_index = -1;
//		fitness�� ū�� ������ ������ �������� ���� �޶������ ������ �켱�� ������ ���ٰ� ��
		for(int i=0;i<population.size();i++) {
			if(population.get(i).fitness_value < best_solution) {
				best_solution = population.get(i).fitness_value;
				best_index = i;
			}
		}
		return population.get(best_index);
	}
	
	
	
	
	public Renewal_MicroOrganism eqArrange(Renewal_MicroOrganism chromosome)
	{
		//Renewal_MicroOrganism�ȿ� each_chromosome(PRC������ gene)�� EQ���� �ɰ���
		ArrayList<EQ> EQ_list = new ArrayList<EQ>();
			
		
		// phase1�� ����� �°Ա� List�� ������ִ� �κ�
		
		int[] using_index = new int[11];
		ArrayList<int[]> facil_start_index = new ArrayList<int[]>();
		int[] start_array = {0,0};
		facil_start_index.add(start_array);
		
		//*--joon_fix ������ �ϵ��ڵ����� 
		String prev_index = "BB";
		int cumulated_index = 0;
		
		
		for(int find_start_index = 0;find_start_index < chromosome.each_chromosome.size();find_start_index++) {	
			if(chromosome.each_chromosome.get(find_start_index).getM_strRoom().equals(prev_index)==false){
				prev_index = chromosome.each_chromosome.get(find_start_index).getM_strRoom();
				int[] input_array = {cumulated_index,0};
				facil_start_index.add(input_array);
			}
			cumulated_index++;
		}
		
		for(int[] phase1_result : resultSet.direct_facil_order) {
//		phase1 ����о�ö��� A : 1, B : 2 ...11���� ���� index �ް��� ����
			
			int number_of_installing = phase1_result[1];
			boolean continuing = true;
			
			if(phase1_result[1]==0) {
				continuing = false;
			}
			int calling_index = facil_start_index.get(phase1_result[0]-1)[0];
			int calling_index_remaining = facil_start_index.get(phase1_result[0]-1)[1];	
			while(continuing) {
				// calling_index_remaining index�� 0���� ŭ => �� ���� ��ġ���� ���� ���� ����
				if(calling_index_remaining > 0) {
					
//					��PRC�� EQ���� ��°�� ����
					if(number_of_installing - chromosome.each_chromosome.get(calling_index).getM_lstEQ().size() + calling_index_remaining >= 0) {
						EQ_list.addAll(chromosome.each_chromosome.get(calling_index).getM_lstEQ().subList(calling_index_remaining,chromosome.each_chromosome.get(calling_index).getM_lstEQ().size()));
						number_of_installing = number_of_installing - chromosome.each_chromosome.get(calling_index).getM_lstEQ().size() + calling_index_remaining;
						calling_index_remaining = 0;
						calling_index++;
					}else {
//						�� �������� ���δٰ� �ƴϰ� �Ϻΰ� ���� - �� �ְ� �߸��� index�� ���� =>facil_start_index
						EQ_list.addAll(chromosome.each_chromosome.get(calling_index).getM_lstEQ().subList(calling_index_remaining,calling_index_remaining + number_of_installing));
						int[] revise_array = {calling_index,calling_index_remaining + number_of_installing};
						facil_start_index.set(phase1_result[0]-1,revise_array);  
						continuing = false;
					}
					
				// calling_index_remaining index�� 0 => �� ���� ��ġ���� ���� ���� �ִ°� �ƴϰ� ���� �����ϴ°�
				}else { 
					if(number_of_installing - chromosome.each_chromosome.get(calling_index).getM_lstEQ().size() >= 0) {
//						��PRC�� EQ���� ��°�� ����
						EQ_list.addAll(chromosome.each_chromosome.get(calling_index).getM_lstEQ());
						number_of_installing = number_of_installing - chromosome.each_chromosome.get(calling_index).getM_lstEQ().size();
						calling_index++;
					}else {
//						�� �������� ���δٰ� �ƴϰ� �Ϻΰ� ���� - �� �ְ� �߸��� index�� ���� =>facil_start_index
						EQ_list.addAll(chromosome.each_chromosome.get(calling_index).getM_lstEQ().subList(0,number_of_installing));
						int[] revise_array = {calling_index,number_of_installing};
						facil_start_index.set(phase1_result[0]-1,revise_array);  
						continuing = false;
					}
				}
				if(calling_index>=chromosome.each_chromosome.size()) {
					continuing = false;
				}
			}
		}
		
		
		// �����ġ�� �ϱ� ���� ��ġ������ �迭�� ����.
		String[] EQBatchOrder = new String[EQ_list.size()];

		for (int i = 0; i < EQ_list.size(); i++)
		{
//			EQBatchOrder[i] = population.getChromosomeByName().get(i).getName();
			EQBatchOrder[i] = EQ_list.get(i).getDeviceName();
		}
		/*2019.7.15 Review
		 * ������ ��ġ�� ������ �� ���̴� �κ�:CEQArrangeInRoom
		 * */
		CEQArrangeInRoom arranger = new CEQArrangeInRoom();
		

		// ksn �׽�Ʈ ���ؼ� �ڵ� ���� (2010.0729). �ϼ��� ���� ���� �ʿ���.
		ArrayList<CBay> bayList = null;
		if(CDataSet.RUNTEST) {
//
//			 if (room.getM_strName().equals("CMP"))
//			 {
//			 bayList = mi.go(room, TestData.CMPEQBatchOrder);
//			 } else if (room.getM_strName().equals("METAL"))
//			 {
//			 bayList = mi.go(room, TestData.METALEQbatchOrder);
//			 } else if (room.getM_strName().equals("TEST"))
//			 {
//			 bayList = mi.go(room, TestData.TESTEQBatchOrder);
//			 } else if (room.getM_strName().equals("CLEAN"))
//			 {
//			 bayList = mi.go(room, TestData.CLEANEQBatchOrder);
//			 } else if (room.getM_strName().equals("CVD"))
//			 {
//			 bayList = mi.go(room, TestData.CVDEQBatchOrder);
//			 } else if (room.getM_strName().equals("PHOTO"))
//			 {
//			 bayList = mi.go(room, TestData.PHOTOEQBatchOrder);
//			 } else if (room.getM_strName().equals("DIFF"))
//			 {
//			 bayList = mi.go(room, TestData.DIFFEQBatchOrder);
//			 } else if (room.getM_strName().equals("ETCH"))
//			 {
//			 bayList = mi.go(room, TestData.ETCHEQBatchOrder);
//			 }
		}
		else {
//			CEQArrangeInRoom���� room�� �׳� bay�� ��� room�� ���ϴ����� �����ִ°� ���� �����ε� �; �켱 ������ room���� ��ġ *--joon_fix
			CRoom any_room = new CRoom();
			bayList = arranger.go(any_room, EQBatchOrder);
		}
		
		// Population�� ũ�θ������� EQ�����͸� ��ǥ�� ��ϵǾ� �ִ� EQ Data�� ������.
		chromosome.getChromosomeByName().clear();
		for (int i = 0; i < bayList.size(); i++)
		{
			chromosome.getChromosomeByName().addAll(bayList.get(i).getListEq());
		}
		chromosome.setM_lstBay(bayList);
		return chromosome;
	}	
	
	public Renewal_MicroOrganism calcFitness_renew(Renewal_MicroOrganism chromosome) {
		//ksn Fitness ���� �ʿ�. ���� ���� -> �Ÿ��� ������ ����?

		double fitnessByArea = 0;
//		double fitnessByDistance = 0;
		//���� ���� ��ġ�� ��� ������ ����ϰ� �ٽ� ���� ���´�.
		
		convertEQCoord(chromosome);
		convertEQCoord();
		convertFixedEqCoord();
		convertZoneCoord();
		
		// ���� �ȿ� ���� ��ġ�ϰ� ���յ�(�̵�����, CrossOver)�� ����Ѵ�.
		//fitnessByArea = calFitnessByArea(population, isLastRoom);
		//population.setFitnessByArea(fitnessByArea);

		chromosome = calFitnessByDistance_renew(chromosome);
//		//population.setFitnessByDistance(fitnessByDistance);
		
		
		convertEQCoord(chromosome);
		convertEQCoord();
		convertZoneCoord();
		convertFixedEqCoord();
		return chromosome;
	}
	
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 21 ���� 5:00:15
	 * @date : 2010. 07. 21
	 * @param population
	 * @�����̷� :
	 * @Method ���� : Population�� EQ ���յ�(�̵�����, CrossOver)�� �����.
	 */
	public MicroOrganism calcFitness(MicroOrganism population, CRoom curRoom, boolean isLastRoom)
	{
		//ksn Fitness ���� �ʿ�. ���� ���� -> �Ÿ��� ������ ����?

		double fitnessByArea = 0;
//		double fitnessByDistance = 0;
		//���� ���� ��ġ�� ��� ������ ����ϰ� �ٽ� ���� ���´�.
		
		//convertEQCoord(population);
		convertEQCoord();
		convertFixedEqCoord();
		convertZoneCoord();
		
		// ���� �ȿ� ���� ��ġ�ϰ� ���յ�(�̵�����, CrossOver)�� ����Ѵ�.
		fitnessByArea = calFitnessByArea(population, isLastRoom);
		population.setFitnessByArea(fitnessByArea);

		population = calFitnessByDistance(population, curRoom);
//		population.setFitnessByDistance(fitnessByDistance);
		
		
		//convertEQCoord(population);
		convertEQCoord();
		convertZoneCoord();
		convertFixedEqCoord();
		return population;
	}
	
	
	
//======================= ===============================================================================================================
//	
//======================= ===============================================================================================================
//	
//	���� ���ʹ� ���� �ڵ�
//	
//======================= ===============================================================================================================
//	
//======================= ===============================================================================================================
	
	
	
	public ArrayList<MicroOrganism> run(CRoom room, LogFile logFile)
	{
		System.out.println("** ���� [" + room.getM_strName() + "]�� ���� ���� ��ġ ����");		
		int iGen = 0;
		int var1 = 0, var2 = 0;
		MicroOrganism population;
		// parameter ����
		initGAParameter(room);
		// �ʱ��� ����
		// �Ÿ� �������� ��� ���� ���. ���� �ݿ��� ���� �ڵ� �߰��ؾ� ��.
		// ����: Fitness Function = FF_�Ÿ�
		// ����: FF = FF_�Ÿ� * p + FF_���� * (1-p) �̷��� �ϸ� scale�� �ٸ��ٴ� ������ ����.
		// ���: FF = FF_�Ÿ�/�ִܰŸ� * p + FF_����/�ּҸ��� * (1-p), �ּ� �Ÿ��� �ּ� ������ �������� �� ���� ���� ����.
		// �ּҰŸ��� �ش� Room�� �ٸ� Room �߽ɰ��� ���� �Ÿ�, �ּ� ������ EQ�� ����(������������)�� ��.
		initialize();
		pastFitnessFunctionValue = 1000000000; // fitness func. �ʱⰪ
		currentFitnessFunctionValue = 999999999;
		isBetterSolution = true; // �� ���� ���� ������ ������� ����.
		int searchCnt = 0;
		ArrayList<Integer> searchHistory = new ArrayList<Integer>();
//		for (int i = 0; i < lstPopulations.size(); i++) {
//			System.out.println("getFitnessByDistance():" + lstPopulations.get(i).getFitnessByDistance());	
//		}
		// maxGenerations ��ŭ GA ���� (terminate ����)
		while (iGen < CDataSet.MICRO_MAX_GENERATION)
		{
			iGen++;

			//���� ���� �Ÿ� ������ ��ġ
			sortPopulationsByDistance();
			
			//���� ���� �Ÿ��� �ݼ��ϴ� �����ġ ������ �����
			printOrganism(0);
			//���� ��ġ �ʿ������ ���� ������� �ɷ���
			
			elitism();
			for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE - buffer.size(); i++)
			{
				// select operator�� ���� ������ �� �θ� ����
				var1 = getRandomPopulation();
				var2 = getRandomPopulation();
				// ���� ����
				population = crossover(var1, var2);
				
				// *--joon_fix mutation�� ���� �κп����� �ǹ̰� ��� �ּ�ó�� �صξ����ϴ�
//				population = mutate(population);

				// population�� EQ�� ������ �°� Room�� ��ġ��.
//				population = eqArrange(population);
				
				// Population�� EQ ���յ�(�̵�����, CrossOver)�� �����
				calcFitness(population, room, room.isLastRoom());
				
				// // ������ ���̷� ������ �ڽ� ��ü�� feasible ���� ���� ��� ����

				// boolean isFeasibleChromosome = population.getFeasible(room);
				// if (!isFeasibleChromosome) repair(chromosom);
				buffer.add(population);
			}
			if(isBetterSolution){
				System.out.println("Generation number : " + iGen);
				searchHistory.add(searchCnt);
				searchCnt = 0;
			} else {
				searchCnt++;
			}
			swap();
			
			if(searchCnt>CDataSet.MICRO_SEARCH_LIMIT){
				System.out.println("No better solution in [" + (searchCnt - 1) + "] generations");				
				break; // �� �̻� ã�Ƶ� ���� �������� ������ ����
			}
			//---*
		} // End while
		sortPopulationsByDistance();
		StringBuffer strSearchHistory = new StringBuffer();
		strSearchHistory.append("�� ���� Ƚ�� [").append(searchHistory.size()).append("] ȸ\r\n");
		strSearchHistory.append("���� ����:");
		for (int i = 0; i < searchHistory.size(); i++) {
			strSearchHistory.append("[").append(searchHistory.get(i)).append("]");
		}
		if(logFile!=null) logFile.append(strSearchHistory.toString());
		if(logFile!=null) logFile.append("���� " + room.getM_strName() + "�� ���� �����ġ ���� (�����/����)");
		printOrganism(0, logFile);
		
		if(logFile!=null) logFile.append("���� " + room.getM_strName() + "�� ���� �����ġ ����");
		// ����� ����ϰ� ������.
		finish();
		
		return lstPopulations;
	}
	
//	/**
//	 * 
//	 * @author : jwon.cho
//	 * @version : 2011. 05. 18 ���� 9:54:34
//	 * @param logFile 
//	 * @date : 2011. 05. 18
//	 * @�����̷� :
//	 * @Method ���� : GA�� Run �ϴ� Method
//	 */
//	public ArrayList<MicroOrganism> run(CRoom room, LogFile logFile, ArrayList<MicroOrganism> initPopulation, int currentRoomIndex)
//	{
//System.out.println("** ���� [" + room.getM_strName() + "]�� ���� ���� ��ġ ����");		
//		int iGen = 0;
//		int var1 = 0, var2 = 0;
//		MicroOrganism population;
//		// parameter ����
//		initGAParameter(room);
//		// �ʱ��� ����
//		// �Ÿ� �������� ��� ���� ���. ���� �ݿ��� ���� �ڵ� �߰��ؾ� ��.
//		// ����: Fitness Function = FF_�Ÿ�
//		// ����: FF = FF_�Ÿ� * p + FF_���� * (1-p) �̷��� �ϸ� scale�� �ٸ��ٴ� ������ ����.
//		// ���: FF = FF_�Ÿ�/�ִܰŸ� * p + FF_����/�ּҸ��� * (1-p), �ּ� �Ÿ��� �ּ� ������ �������� �� ���� ���� ����.
//		// �ּҰŸ��� �ش� Room�� �ٸ� Room �߽ɰ��� ���� �Ÿ�, �ּ� ������ EQ�� ����(������������)�� ��.
//		initialize(initPopulation);
//		pastFitnessFunctionValue = 1000000000; // fitness func. �ʱⰪ
//		currentFitnessFunctionValue = 999999999;
//		isBetterSolution = true; // �� ���� ���� ������ ������� ����.
//		int searchCnt = 0;
//		ArrayList<Integer> searchHistory = new ArrayList<Integer>();
////		for (int i = 0; i < lstPopulations.size(); i++) {
////			System.out.println("getFitnessByDistance():" + lstPopulations.get(i).getFitnessByDistance());	
////		}
//		// maxGenerations ��ŭ GA ���� (terminate ����)
//		while (iGen < CDataSet.MICRO_MAX_GENERATION)
//		{
//			iGen++;
//
//			//���� ���� �Ÿ� ������ ��ġ
//			sortPopulationsByDistance();
//			
//			//���� ���� �Ÿ��� �ݼ��ϴ� �����ġ ������ �����
//			printOrganism(0);
//			//���� ��ġ �ʿ������ ���� ������� �ɷ���
//			
//			elitism();
//			for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE - buffer.size(); i++)
//			{
//				// select operator�� ���� ������ �� �θ� ����
//				var1 = getRandomPopulation();
//				var2 = getRandomPopulation();
//				// ���� ����
//				population = crossover(var1, var2);
//				population = mutate(population);
//
//				// population�� EQ�� ������ �°� Room�� ��ġ��.
//				population = eqArrange(population);
//				
//				// Population�� EQ ���յ�(�̵�����, CrossOver)�� �����
//				calcFitness(population, room, room.isLastRoom());
//				
//				// // ������ ���̷� ������ �ڽ� ��ü�� feasible ���� ���� ��� ����
//
//				// boolean isFeasibleChromosome = population.getFeasible(room);
//				// if (!isFeasibleChromosome) repair(chromosom);
//				buffer.add(population);
//			}
//			if(isBetterSolution){
//				System.out.println("Generation number : " + iGen);
//				searchHistory.add(searchCnt);
//				searchCnt = 0;
//			} else {
//				searchCnt++;
//			}
//			swap();
//			if(searchCnt>CDataSet.MICRO_SEARCH_LIMIT){
//System.out.println("No better solution in [" + (searchCnt - 1) + "] generations");				
//				break; // �� �̻� ã�Ƶ� ���� �������� ������ ����
//			}
//		} // End while
//		sortPopulationsByDistance();
//		StringBuffer strSearchHistory = new StringBuffer();
//		strSearchHistory.append("�� ���� Ƚ�� [").append(searchHistory.size()).append("] ȸ\r\n");
//		strSearchHistory.append("���� ����:");
//		for (int i = 0; i < searchHistory.size(); i++) {
//			strSearchHistory.append("[").append(searchHistory.get(i)).append("]");
//		}
//		if(logFile!=null) logFile.append(strSearchHistory.toString());
//		if(logFile!=null) logFile.append("���� " + room.getM_strName() + "�� ���� �����ġ ���� (�����/����)");
//		printOrganism(0, logFile);
//		
//		if(logFile!=null) logFile.append("���� " + room.getM_strName() + "�� ���� �����ġ ����");
//		// ����� ����ϰ� ������.
//		finish();
//		
//		return lstPopulations;
//	}
	
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 13 ���� 9:54:34
	 * @param logFile 
	 * @date : 2010. 07. 13
	 * @�����̷� : jwon.cho �߰� - ���� ���̽�
	 * @Method ���� : GA�� Run �ϴ� Method
	 */
	public ArrayList<MicroOrganism> runArea(CRoom room, LogFile logFile)
	{
		int iGen = 0;
		int var1 = 0, var2 = 0;
		MicroOrganism population;
		// parameter ����
		initGAParameter(room);
		// �ʱ��� ����
		// �Ÿ� �������� ��� ���� ���. ���� �ݿ��� ���� �ڵ� �߰��ؾ� ��.
		// ����: Fitness Function = FF_�Ÿ�
		// ����: FF = FF_�Ÿ� * p + FF_���� * (1-p) �̷��� �ϸ� scale�� �ٸ��ٴ� ������ ����.
		// ���: FF = FF_�Ÿ�/�ִܰŸ� * p + FF_����/�ּҸ��� * (1-p), �ּ� �Ÿ��� �ּ� ������ �������� �� ���� ���� ����.
		// �ּҰŸ��� �ش� Room�� �ٸ� Room �߽ɰ��� ���� �Ÿ�, �ּ� ������ EQ�� ����(������������)�� ��.
		initialize();
		pastFitnessFunctionValue = 0; // fitness func. �ʱⰪ
		currentFitnessFunctionValue = 1;
		isBetterSolution = true; // �� ���� ���� ������ ������� ����.
		int searchCnt = 0;
//		for (int i = 0; i < lstPopulations.size(); i++) {
//			System.out.println("getFitnessByDistance():" + lstPopulations.get(i).getFitnessByDistance());	
//		}
		// maxGenerations ��ŭ GA ���� (terminate ����)
		while (iGen < CDataSet.MICRO_MAX_GENERATION)
		{
			iGen++;

			//���� ���� �Ÿ� ������ ��ġ
			sortPopulationsByArea();
			
			//���� ���� �Ÿ��� �ݼ��ϴ� �����ġ ������ �����
			printOrganismArea(0);
			//���� ��ġ �ʿ������ ���� ������� �ɷ���
			elitism();
			for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE - buffer.size(); i++)
			{
				// select operator�� ���� ������ �� �θ� ����
				var1 = getRandomPopulationArea();
				var2 = getRandomPopulationArea();
				// ���� ����
				population = crossover(var1, var2);
				population = mutate(population);

				// population�� EQ�� ������ �°� Room�� ��ġ��.
//				population = eqArrange(population);
				
				// Population�� EQ ���յ�(�̵�����, CrossOver)�� �����
				calcFitness(population, room, room.isLastRoom());
				
				// // ������ ���̷� ������ �ڽ� ��ü�� feasible ���� ���� ��� ����

				// boolean isFeasibleChromosome = population.getFeasible(room);
				// if (!isFeasibleChromosome) repair(chromosom);
				buffer.add(population);
			}
			if(isBetterSolution){
				System.out.println("Generation number : " + iGen);
				searchCnt = 0;
			} else {
				searchCnt++;
			}
			swap();
			if(searchCnt>CDataSet.MICRO_SEARCH_LIMIT){
System.out.println("searchCnt:" + searchCnt);				
				break; // �� �̻� ã�Ƶ� ���� �������� ������ ����
			}
		} // End while
		sortPopulationsByArea();
		if(logFile!=null) logFile.append("���� " + room.getM_strName() + "�� ���� �����ġ ���� (�����/����)");
		printOrganismArea(0, logFile);
		
		if(logFile!=null) logFile.append("���� " + room.getM_strName() + "�� ���� �����ġ ����");
		// ����� ����ϰ� ������.
		finishArea();
		
		return lstPopulations;
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 21 ���� 4:56:41
	 * @date : 2010. 07. 21
	 * @param population
	 * @�����̷� :
	 * @Method ���� : Eq�� ������ �°� ��ġ��.
	 */
	/*
	 * EQBatchOrder�� EQ ������ String���� ���� �ִ�.
	 * �ߺ�..
	 * */
	

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 ���� 8:50:40
	 * @date : 2010. 07. 19
	 * @�����̷� :
	 * @Method ���� : �α�(Population, ����ü�� ����) ������ ���� ���� �α�(Population, ����ü�� ����) ������ �����մϴ�. �� ���ο� ����(New Generation)�� ����ϴ�.
	 */
	private void swap()
	{
		lstPopulations.clear();
		lstPopulations.addAll(buffer);
		buffer.clear();
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 ���� 9:00:33
	 * @date : 2010. 07. 19
	 * @param row
	 * @�����̷� :
	 * @Method ���� : row ��° Chromosome�� �迭�� ������ �Է¹���. �ذ� ������ ��� ���
	 */
	private void printOrganism(int row)
	{
		MicroOrganism c = (MicroOrganism) lstPopulations.get(row);
		ArrayList<Integer> chromosome = c.getChromosomeByInt();
		
		currentFitnessFunctionValue = c.getIncrementalFitnessByDistance();
		if(currentFitnessFunctionValue < pastFitnessFunctionValue) {
			pastFitnessFunctionValue = currentFitnessFunctionValue;
			isBetterSolution = true;
		} else {
			isBetterSolution = false;
		}
		
		if(isBetterSolution) {
			// System.out.print("[" + c.getFitnessByArea() + "]");
			StringBuffer prtStr = new StringBuffer();
			prtStr.append("[fitness:" + c.getFitnessByDistance() + "/ "+ c.getFitnessByDistance()/CDataSet.getInstance().fab_total_volume*1000 +"]");
			prtStr.append("[incrementalF:" + c.getIncrementalFitnessByDistance() + "/ "+ c.getIncrementalFitnessByDistance()/CDataSet.getInstance().fab_total_volume*1000 +"]");
			for (int i = 0; i < chromosome.size(); i++)
				prtStr.append(chromosome.get(i) + " ");
			prtStr.append("\n");
			System.out.println(prtStr.toString());
		}
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 ���� 9:00:33
	 * @date : 2010. 07. 19
	 * @param row
	 * @�����̷� :
	 * @Method ���� : row ��° Chromosome�� �迭�� ������ �Է¹���. �ظ� ���Ϸ� ���
	 */
	private void printOrganism(int row, LogFile logFile)
	{
		MicroOrganism c = (MicroOrganism) lstPopulations.get(row);
		ArrayList<Integer> chromosome = c.getChromosomeByInt();
		ArrayList<EQ> chromosomeEQ = c.getChromosomeByName();
		
		StringBuffer prtStr = new StringBuffer();
		prtStr.append("[fitness:" + c.getFitnessByDistance() + "/ "+ c.getFitnessByDistance()/CDataSet.getInstance().fab_total_volume*1000 +"]");
		prtStr.append("[incrementalF:" + c.getIncrementalFitnessByDistance() + "/ "+ c.getIncrementalFitnessByDistance()/CDataSet.getInstance().fab_total_volume*1000 +"]");
//		prtStr.append("[" + c.getFitnessByDistance() + "]");
		for (int i = 0; i < chromosome.size(); i++)
			prtStr.append(chromosome.get(i) + " ");
		prtStr.append("\n");
		System.out.println(prtStr.toString());
		
		StringBuffer logStr = new StringBuffer();
		for (int i = 0; i < chromosomeEQ.size(); i++)
			logStr.append(chromosomeEQ.get(i).getModule() + "/" + chromosomeEQ.get(i).getDeviceName() + " ");
		
		if(logFile!=null) logFile.append(logStr.toString());

	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 ���� 9:00:33
	 * @date : 2010. 07. 19
	 * @param row
	 * @�����̷� :
	 * @Method ���� : row ��° Chromosome�� �迭�� ������ �Է¹���. �ذ� ������ ��� ���
	 */
	private void printOrganismArea(int row)
	{
		MicroOrganism c = (MicroOrganism) lstPopulations.get(row);
		ArrayList<Integer> chromosome = c.getChromosomeByInt();
		ArrayList<EQ> chromosomeEQ = c.getChromosomeByName();
		
		currentFitnessFunctionValue = c.getFitnessByArea();
		if(currentFitnessFunctionValue < pastFitnessFunctionValue) {
			pastFitnessFunctionValue = currentFitnessFunctionValue;
			isBetterSolution = true;
		} else {
			isBetterSolution = false;
		}
		
		if(isBetterSolution) {
			// System.out.print("[" + c.getFitnessByArea() + "]");
			StringBuffer prtStr = new StringBuffer();
			prtStr.append("[" + c.getFitnessByArea() + "]");
			for (int i = 0; i < chromosome.size(); i++)
				prtStr.append(chromosome.get(i) + " ");
			prtStr.append("\n");
			System.out.println(prtStr.toString());
		}
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 ���� 9:00:33
	 * @date : 2010. 07. 19
	 * @param row
	 * @�����̷� :
	 * @Method ���� : row ��° Chromosome�� �迭�� ������ �Է¹���. �ظ� ���Ϸ� ���
	 */
	private void printOrganismArea(int row, LogFile logFile)
	{
		MicroOrganism c = (MicroOrganism) lstPopulations.get(row);
		ArrayList<Integer> chromosome = c.getChromosomeByInt();
		ArrayList<EQ> chromosomeEQ = c.getChromosomeByName();
		
		StringBuffer prtStr = new StringBuffer();
		prtStr.append("[" + c.getFitnessByArea() + "]");
		for (int i = 0; i < chromosome.size(); i++)
			prtStr.append(chromosome.get(i) + " ");
		prtStr.append("\n");
		System.out.println(prtStr.toString());
		
		StringBuffer logStr = new StringBuffer();
		for (int i = 0; i < chromosomeEQ.size(); i++)
			logStr.append(chromosomeEQ.get(i).getModule() + "/" + chromosomeEQ.get(i).getDeviceName() + " ");
		
		if(logFile!=null) logFile.append(logStr.toString());

	}
	/**
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 ���� 9:31:07
	 * @date : 2010. 07. 19
	 * @�����̷� :
	 * @Method ���� : fitness Rank�� ����� ������ ������������ ������.
	 */
	private void sortPopulationsByArea()
	{
		Collections.sort(lstPopulations, new AreaComparator());

		Collections.reverse(lstPopulations);

	}
	/**
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 ���� 9:31:07
	 * @date : 2010. 07. 19
	 * @�����̷� :
	 * @Method ���� : fitness Rank�� ����� ������ ������������ ������.
	 */
	private void sortPopulationsByDistance()
	{
		Collections.sort(lstPopulations, new DistanceComparator());
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 12 ���� 5:04:26
	 * @date : 2010. 07. 12
	 * @param maxgenera
	 *            �ִ� �ڼ��� ��
	 * @param popul
	 *            ������
	 * @param room
	 *            Room�� ������
	 * @param fab
	 *            �г��� ����.
	 * @�����̷� :
	 * @Method ���� : parameter ����
	 */
	private void initGAParameter(CRoom ro)
	{
		// �������� ����ŭ chromosom�� �����ϱ� ���� �迭�� Chromosom List ����.
		lstPopulations = new ArrayList<MicroOrganism>();
		room = ro;
		moduleList = room.getM_lstModule();
//		lstPopulations = new ArrayList<MicroOrganism>();
		buffer = new ArrayList<MicroOrganism>();

		propertiesByInt = new HashMap<Integer, CModule>();
		propertiesByName = new HashMap<String, Integer>();

		// ������ ���� �ϳ��� mapping
		int putSize = moduleList.size();
		for (int i = 0; i < putSize; i++)
		{
			propertiesByInt.put(i, moduleList.get(i));
			propertiesByName.put(moduleList.get(i).getM_strName(), i);

		}

		int moduleNum = room.getM_nModuleCount(); // ����� ��
		moduleEqNum = new int[moduleNum]; // ��⺰ ������� ��

		for (int j = 0; j < moduleNum; j++)
		{
			moduleEqNum[j] = room.getM_lstModule().get(j).getM_nEQCount();
		}
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 13 ���� 9:45:22
	 * @date : 2010. 07. 13
	 * @�����̷� :
	 * @Method ���� : �ʱ��� ����, GA�� ����ϱ� ���� �־��� layout������ ����ũ�� �� ������ random�ϰ� population�� ���������, ������ layout ������ repair
	 *         operation �̳� random�� �ƴ� �ٸ� ������� initial solutions �� �����ϴ� ���� ���� ������ ����
	 * 
	 */
	private void initialize()
	{
		int EqNum = room.getM_lstEQ().size(); // ��ü ������� ��
		int moduleNum = room.getM_lstModule().size(); // ����� ��
		int[] tempModuleEqNum = new int[moduleNum]; // ��⺰ �����
		boolean isLastRoom = room.isLastRoom();
		ArrayList<Integer> chromosomeByInt;
		ArrayList<EQ> chromosomeByName;

		MicroOrganism population;

		/*2019.07.11 �Ӵ��� review
		 * chromosomeByInt�� ����ȣ�� ��´�. ���� ��� 3,1,0,2,2
		 * 1st ����� 3�� ��⿡�� ������� ���
		 * 2nd ����� 1�� ��⿡�� ������� ���
		 * ����� PRC �׷��̶�� �̷����ϸ� �ȵɵ�! �̶��� ����� �� ���� ��������..
		 * */
		// ��⺰ ������� ���� �ʰ� �ʱ��ظ� ���ϴ� ����
		for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE; i++)
		{
			chromosomeByInt = new ArrayList<Integer>(EqNum); // solution
			chromosomeByName = new ArrayList<EQ>(EqNum); // solution

			for (int j = 0; j < moduleNum; j++)
				tempModuleEqNum[j] = moduleEqNum[j];

			for (int j = 0; j < EqNum; j++)
			{ // ��⺰ ������� ���� ���� �ʴ� �ѿ��� �����ϰ� ��ġ
				int k;
				do
				{
					k = new Random().nextInt(moduleNum);
				} while (tempModuleEqNum[k] <= 0);

				chromosomeByInt.add(k);
				tempModuleEqNum[k]--;
			}

			// ���� ��ġ ������ ���� ���·� �Ǿ� �ִ� ���� �̸����� Mapping �ϴ� �۾�.
			chromosomeByName = eqMappingToName(chromosomeByInt, chromosomeByName);

			// populatin ����.
			/*�Ӵ��� review
			 * chromosomeByInt�� EQ�� ��� ��⿡�� �������� ���� �ִ�.
			 * chromosomeByName�� EQ�� ������ �̸����� ���� �ִ�.
			 * propertiesByInt�� ���ڰ� � ��������� ���� �ִ�.
			 * */
			population = new MicroOrganism(chromosomeByInt, chromosomeByName, propertiesByInt);

			// population�� EQ�� ������ �°� Room�� ��ġ��.
//			eqArrange(population);

			// Population�� EQ ���յ�(�̵�����, CrossOver)�� �����
			calcFitness(population, room, room.isLastRoom());

			lstPopulations.add(population);

		}// End For (int i = 0; i < population; i++)

//		System.out.println("�ʱ�ȭ �Ϸ�");
	}
//	/**
//	 * 
//	 * @author : jwon.cho
//	 * @version : 2011.05.18
//	 * @date : 2011.05.18
//	 * @�����̷� :
//	 * @Method ���� : �ʱ��ذ� �־��� ���(�̹� ��� �� �� �� ���� Micro Popuation Size��ŭ �̹� �ظ� ���� ����)
//	 * 
//	 */
//	private void initialize(ArrayList<MicroOrganism> initPopulation)
//	{
//		MicroOrganism population;
//		
//		for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE; i++)
//		{
//			// populatin ����.
//			population = new MicroOrganism(initPopulation.get(i).getChromosomeByInt(), initPopulation.get(i).getChromosomeByName(), propertiesByInt);
//
//			// population�� EQ�� ������ �°� Room�� ��ġ��.
//			eqArrange(population);
//
//			// Population�� EQ ���յ�(�̵�����, CrossOver)�� �����
//			calcFitness(population, room, room.isLastRoom());
//
//			lstPopulations.add(population);
//
//		}// End For (int i = 0; i < population; i++)
//
////		System.out.println("�ʱ�ȭ �Ϸ�");
//	}

	
	/**
	 * Zone�� �������� �������� zone�� ��Ī�̵��Ѵ�.
	 *  y=x�� ���� ��Ī�̵� / ������(left, top)
	 */
	public void convertZoneCoord() {
		for (int i = 0; i < dataSet.getM_lstZone().size(); i++) {
			CZone zone = dataSet.getM_lstZone().get(i);
			if(zone.getType().equals("����")){
				double top, width, height;
				width = zone.getWidth();
				height = zone.getHeight();
				zone.setWidth(height);
				zone.setHeight(width);
			}
		}
	}
	/**
	 * fixedEq ������
	 */
	public void convertFixedEqCoord() {
		double zoneLeft, zoneTop, zoneHeight, zoneWidth, datumPointX, datumPointY, fixedEqLeft, fixedEqTop, fixedEqWidth, fixedEqHeight;
		CZone zone = new CZone();
		
		for (int i = 0; i < dataSet.getM_lstFixedEq().size(); i++){
			EQ fixedEq = dataSet.getM_lstFixedEq().get(i);
			
			for (int m = 0; m < dataSet.getM_lstZone().size(); m++) {
				if(dataSet.getM_lstZone().get(m).getZoneIndex() == fixedEq.getZoneIndex()) 
					zone = dataSet.getM_lstZone().get(m);
			}
			if(zone.getType().equals("����")){
				zoneTop = zone.getTop();
				zoneLeft = zone.getLeft();
				zoneHeight = zone.getHeight();
				zoneWidth = zone.getWidth();
				fixedEqLeft =  fixedEq.getCoordLeft();
				fixedEqTop = fixedEq.getCoordTop();
				fixedEqWidth = fixedEq.getCoordWidth();
				
				fixedEqHeight = fixedEq.getCoordLength();
				
				datumPointX = zoneLeft;
				datumPointY = zoneTop;
				fixedEq.setCoordLeft(fixedEqTop-datumPointY+datumPointX);
				fixedEq.setCoordTop(fixedEqLeft-datumPointX+datumPointY);
				fixedEq.setCoordLength(fixedEqWidth);
				fixedEq.setCoordWidth(fixedEqHeight);
			}
		}
	}
	/**
	 * room ������ eq ������
	 */
	public void convertEQCoord() {
		double zoneLeft, zoneTop, zoneHeight, zoneWidth, datumPointX, datumPointY, eqLeft, eqTop, eqWidth, eqHeight;
		for (int i = 0; i < resultSet.getM_bestRoomOrder().getChromosomeByName().size(); i++)
		{
			CRoom room = resultSet.getM_bestRoomOrder().getChromosomeByName().get(i);
//			if(room.????.size()>0){ // room�� �ʹ� ���� bay�� �������� ���� �� ���� jwon.cho 2010-12-16
//				for (int j = 0; j < room.getM_lstBayResult().get(0).getM_lstBay().size(); j++)
//				{
//					CBay bay = room.getM_lstBaynewResult().get(0).getM_lstBay().get(j);
//					for (int k = 0; k < bay.getListEq().size(); k++)
			
			if(room.getM_lstBaynewResult().getM_lstBay().size()>0){ // room�� �ʹ� ���� bay�� �������� ���� �� ���� jwon.cho 2010-12-16
			for (int j = 0; j < room.getM_lstBaynewResult().getM_lstBay().size(); j++)
			{
				CBay bay = room.getM_lstBaynewResult().getM_lstBay().get(j);
				for (int k = 0; k < bay.getListEq().size(); k++)
					{
						EQ eq = bay.getListEq().get(k);
						CZone zone = new CZone();
						for (int m = 0; m < dataSet.getM_lstZone().size(); m++) {
							if(dataSet.getM_lstZone().get(m).getZoneIndex()==bay.getZone()) 
								//eq�� zone�� ����ߴ���, zone�� �ٲ���� �� ù EQ�� ���� zoneIndex�� ������ �־ �̻���
								zone = dataSet.getM_lstZone().get(m);
						}
						if(zone.getType().equals("����")){
							zoneTop = zone.getTop();
							zoneLeft = zone.getLeft();
							zoneHeight = zone.getHeight();
							zoneWidth = zone.getWidth();
							eqLeft =  eq.getCoordLeft();
							eqTop = eq.getCoordTop();
							eqWidth = eq.getCoordWidth();
							eqHeight = eq.getCoordLength();
							
							datumPointX = zoneLeft;
							datumPointY = zoneTop;
							eq.setCoordLeft(eqTop-datumPointY+datumPointX);
							eq.setCoordTop(eqLeft-datumPointX+datumPointY);
							eq.setCoordLength(eqWidth);
							eq.setCoordWidth(eqHeight);
						}
					}
				}
			}
		}
	}
	private Renewal_MicroOrganism convertEQCoord(Renewal_MicroOrganism population) {
		ArrayList<EQ> EQList = population.getChromosomeByName();
		double zoneLeft, zoneTop, zoneHeight, zoneWidth, datumPointX, datumPointY, eqLeft, eqTop, eqWidth, eqHeight;
		for (int k = 0; k < EQList.size(); k++)
		{
			EQ eq = EQList.get(k);
			CZone zone = new CZone();
			zone = getZone(eq);
			
			if(zone.getType().equals("����")){
				zoneTop = zone.getTop();
				zoneLeft = zone.getLeft();
				zoneHeight = zone.getHeight();
				zoneWidth = zone.getWidth();
				eqLeft =  eq.getCoordLeft();
				eqTop = eq.getCoordTop();
				eqWidth = eq.getCoordWidth();
				eqHeight = eq.getCoordLength();
	
				datumPointX = zoneLeft;
				datumPointY = zoneTop;
				eq.setCoordLeft(eqTop-datumPointY+datumPointX);
				eq.setCoordTop(eqLeft-datumPointX+datumPointY);
				eq.setCoordLength(eqWidth);
				eq.setCoordWidth(eqHeight);
			}
			
		}
		return population;
	}
	private CZone getZone(EQ eq) {
		CZone zone = null;
		for (int m = 0; m < dataSet.getM_lstZone().size(); m++) {
			if(dataSet.getM_lstZone().get(m).getZoneIndex()==eq.getZoneIndex()) 
				//eq�� zone�� ����ߴ���, zone�� �ٲ���� �� ù EQ�� ���� zoneIndex�� ������ �־ �̻���
				zone = dataSet.getM_lstZone().get(m);
		}
		return zone;
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 21 ���� 5:24:32
	 * @date : 2010. 07. 21
	 * @param population
	 * @return
	 * @�����̷� :
	 * @Method ���� : ���� ���� �������� �ּ�ȭ. Zone�� �������� ������ Bay�� ��ġ�� ���� ����Ѵ�.
	 * zoneRemainSize�� �Ź� ���ؼ� �ʹ� ū ���� ������.  �������� �� ���� ���ؾ� ��. jwon.cho
	 */
	private double calFitnessByArea(MicroOrganism population, boolean isLastRoom)
	{
		
		int bayIndex = population.getM_lstBay().size();
		CBay bay = population.getM_lstBay().get(bayIndex-1);
		int zoneIndex = bay.getZone();
		boolean isBayUpperDirection = bay.isM_isBayUpperdirection();
		boolean isEqUpperDirection = bay.isM_isEQUpperDirection();
		
		double zoneWidth = dataSet.getM_lstZone().get(zoneIndex).getWidth(); 
		double zoneHeight = dataSet.getM_lstZone().get(zoneIndex).getHeight();
		double zoneTop = dataSet.getM_lstZone().get(zoneIndex).getTop();
		double zoneRemainHeight=0.0f;
		double bayRemainSize = 0.0f;
		double bayHeight = 0.0f;
		double zoneRemainSize=0.0f;
		double remainSize=0.0f;
		double bayTop = bay.getTop();
		
		double unusedArea=0.0f;
		
		if(isBayUpperDirection) {
			if(isEqUpperDirection)
			{
				// Bay ������� ��� ����
				int eqCnt = bay.getListEq().size();
				for (int i = 0; i < eqCnt; i++)
				{
					bayHeight = bayHeight + bay.getListEq().get(i).getCoordLength();
				}
				bayHeight = bayHeight/(double)eqCnt;
				
				// Bay���� ���� �����ϰ� ���� ����
				bayRemainSize = bayHeight * bay.getRemainWidth();
				
				// zone���� ���� ������ ����
				zoneRemainHeight = zoneTop + zoneHeight - bayTop;	
				
				// zone���� ���� ����
				zoneRemainSize = zoneRemainHeight * zoneWidth;
				
				// ��ü ���� ����
				remainSize = bayRemainSize + zoneRemainSize;
				if(isLastRoom) unusedArea = bayRemainSize + zoneRemainSize;
				else unusedArea = bayRemainSize;
				 
//				System.out.println("1");			
//				System.out.println("bayHeight: " + bayHeight + "	bay.getRemainWidth(): " + bay.getRemainWidth() + "	zoneTop: " + zoneTop + 
//						"	zoneHeight: " + zoneHeight + "	bayTop: " + bayTop + "zoneRemainHeight: " + zoneRemainHeight + "	zoneWidth: " + zoneWidth);							
//System.out.println("isBayUpperDirection: " + isBayUpperDirection + "	isEqUpperDirection: " + isEqUpperDirection + "	bayRemainSize: " + bayRemainSize/10000 + 
//"	zoneRemainSize: " + zoneRemainSize/10000 + "	remainSize: " + remainSize/10000);				
			}else 
			{
				// Bay ���񿡼� ���̰� ���� ū ������ ����
				int eqCnt = bay.getListEq().size();
				for (int i = 0; i < eqCnt; i++)
				{
					if (bayHeight < bay.getListEq().get(i).getCoordLength())
						bayHeight = bay.getListEq().get(i).getCoordLength();
				}
				
				// Bay���� ���� �����ϰ� ���� ����
				bayRemainSize = bayHeight * bay.getRemainWidth();
				
				// zone���� ���� ������ ����
				zoneRemainHeight = zoneTop + zoneHeight - (bayTop + bayHeight);	
				
				// zone���� ���� ����
				zoneRemainSize = zoneRemainHeight * zoneWidth;
				
				// ��ü ���� ����
				remainSize = bayRemainSize + zoneRemainSize;
				if(isLastRoom) unusedArea = bayRemainSize + zoneRemainSize;
				else unusedArea = bayRemainSize;
//				System.out.println("2");			
//				System.out.println("bayHeight: " + bayHeight + "	bay.getRemainWidth(): " + bay.getRemainWidth() + "	zoneTop: " + zoneTop + 
//						"	zoneHeight: " + zoneHeight + "	bayTop: " + bayTop + "zoneRemainHeight: " + zoneRemainHeight + "	zoneWidth: " + zoneWidth);
//				System.out.println("isBayUpperDirection: " + isBayUpperDirection + "	isEqUpperDirection: " + isEqUpperDirection + "	bayRemainSize: " + bayRemainSize/10000 + 
//						"	zoneRemainSize: " + zoneRemainSize/10000 + "	remainSize: " + remainSize/10000);				
			}
		} else 
		{
			if(isEqUpperDirection)
			{
				// Bay ���񿡼� ���̰� ���� ū ������ ����
				int eqCnt = bay.getListEq().size();
				for (int i = 0; i < eqCnt; i++)
				{
					if (bayHeight < bay.getListEq().get(i).getCoordLength())
						bayHeight = bay.getListEq().get(i).getCoordLength();
				}
				
				// Bay���� ���� �����ϰ� ���� ����
				bayRemainSize = bayHeight * bay.getRemainWidth();
				
				// zone���� ���� ������ ����
				zoneRemainHeight = (bayTop - bayHeight) - zoneTop;	
				
				// zone���� ���� ����
				zoneRemainSize = zoneRemainHeight * zoneWidth;
				
				// ��ü ���� ����
				remainSize = bayRemainSize + zoneRemainSize;
				if(isLastRoom) unusedArea = bayRemainSize + zoneRemainSize;
				else unusedArea = bayRemainSize;
//				System.out.println("3");			
//				System.out.println("bayHeight: " + bayHeight + "	bay.getRemainWidth(): " + bay.getRemainWidth() + "	zoneTop: " + zoneTop + 
//						"	zoneHeight: " + zoneHeight + "	bayTop: " + bayTop + "zoneRemainHeight: " + zoneRemainHeight + "	zoneWidth: " + zoneWidth);
//				System.out.println("isBayUpperDirection: " + isBayUpperDirection + "	isEqUpperDirection: " + isEqUpperDirection + "	bayRemainSize: " + bayRemainSize/10000 + 
//						"	zoneRemainSize: " + zoneRemainSize/10000 + "	remainSize: " + remainSize/10000);				
				
			}else 
			{
				// Bay ������� ��� ����
				int eqCnt = bay.getListEq().size();
				for (int i = 0; i < eqCnt; i++)
				{
					bayHeight = bayHeight + bay.getListEq().get(i).getCoordLength();
				}
				bayHeight = bayHeight/(double)eqCnt;
				
				// Bay���� ���� �����ϰ� ���� ����
				bayRemainSize = bayHeight * bay.getRemainWidth();
				
				// zone���� ���� ������ ����
				zoneRemainHeight = bayTop - zoneTop;	
				
				// zone���� ���� ����
				zoneRemainSize = zoneRemainHeight * zoneWidth;
				
				// ��ü ���� ����
				remainSize = bayRemainSize + zoneRemainSize;
				if(isLastRoom) unusedArea = bayRemainSize + zoneRemainSize;
				else unusedArea = bayRemainSize;
//				System.out.println("4");			
//				System.out.println("bayHeight: " + bayHeight + "	bay.getRemainWidth(): " + bay.getRemainWidth() + "	zoneTop: " + zoneTop + 
//						"	zoneHeight: " + zoneHeight + "	bayTop: " + bayTop + "zoneRemainHeight: " + zoneRemainHeight + "	zoneWidth: " + zoneWidth);
//				System.out.println("isBayUpperDirection: " + isBayUpperDirection + "	isEqUpperDirection: " + isEqUpperDirection + "	bayRemainSize: " + bayRemainSize/10000 + 
//						"	zoneRemainSize: " + zoneRemainSize/10000 + "	remainSize: " + remainSize/10000);				
			}
		}
		population.setUnusedArea(unusedArea/1000000);
		return remainSize/1000000;
	}
	
	// ���ο� �Ÿ� fitness ��� �Լ�
	public Renewal_MicroOrganism calFitnessByDistance_renew(Renewal_MicroOrganism chromosome) {
		// hashmap���� ������ ¦�� ����
		// ���� ������ �߻��ϸ�, �ش� row���� ������ ���� Ȧ���̾ �߻��ϴ� ����
		Map <Integer,Integer> bay_couple=new HashMap<Integer,Integer>();
		for(int i=0;i<chromosome.m_lstBay.size()-1;i+=2) {
			if (chromosome.m_lstBay.get(i).getZone()==chromosome.m_lstBay.get(i+1).getZone()) {
				bay_couple.put(i,i+1);
				bay_couple.put(i+1,i);
			}
		}
				
		// chromosome�� �� ���̸� �д´�.
		
		double total_dist=0;
		
		for(int i=0;i<chromosome.m_lstBay.size();i++) {
			CBay frombay=chromosome.m_lstBay.get(i);
			// �� ������ eq�� �д´�.
			for(int j=0;j<chromosome.m_lstBay.get(i).getListEq().size();j++) {
				// fromEQ�� ����
				EQ fromEQ=chromosome.m_lstBay.get(i).getListEq().get(j);
				double each_eq_dist=0.0;
				// toEQ�� �д´�.
				for(int k=0;k<chromosome.m_lstBay.size();k++) {
					// tobay�� ����
					CBay tobay=chromosome.m_lstBay.get(k);
					for(int l=0;l<chromosome.m_lstBay.get(k).getListEq().size();l++) {						
						EQ toEQ=chromosome.m_lstBay.get(k).getListEq().get(l);
						double freq=0;
						String fromEQName=fromEQ.getDeviceName();
						String toEQName=toEQ.getDeviceName();
						String key = fromEQName + "$" + toEQName;
						if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
						else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());
						if (fromEQ!=toEQ && freq>0) {
							each_eq_dist+=freq*distance_fitness(fromEQ,toEQ,frombay,tobay,i,k,bay_couple,chromosome);
						}
						else {
							each_eq_dist+=0;
						}
					}
				}
				total_dist+=each_eq_dist;
			}
		}
		
		chromosome.set_fitness_value(total_dist);
		
		return chromosome;
	}
	
	public double distance_fitness(EQ fromEQ, EQ toEQ, CBay frombay, CBay tobay, int i, int k, Map<Integer,Integer> bay_couple, Renewal_MicroOrganism chromosome) {
		double eqtozone=0;
		double zonetozone=0;
		double zonetoeq=0;
		// ���� ���̳� �� �Ǵ� �쿡 ���� �ִ� ���
		if (frombay==tobay) {
			// ���̰� �Ʒ� �� ������ ���
			if(frombay.isM_isBayUpperdirection()) {
				//�ð� ����
				if(fromEQ.getCoordTop()<toEQ.getCoordTop()) {						
					eqtozone=Math.abs((fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-(toEQ.getCoordTop()+toEQ.getCoordLength()/2));
				}
				// �ð� �ݴ���� (¦ row�� �̵�)
				else {
					// ���̰� �Ʒ��ʿ� �ִ� ���
					if(frombay.isM_isBayUpperdirection()) {
						// 1. ���� ���񿡼� central loop���� ���� �Ÿ�
						eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
						// 2. central loop�� �̵��Ÿ�
						// ���� row�� �θ�
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// �ݴ��� row�� �θ�. ���̰� �Ʒ��� ��ġ�Ǿ��ֱ� ������ index +1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
						// central loop ���� �̵��Ÿ�
						zonetozone+=frombay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop ���� �̵��ϴ� �Ÿ�
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// central loop ���������� �̵��ϴ� �Ÿ�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop �������� �Ÿ�
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// central loop ���� ������ ¦���� �̵��ϴ� �Ÿ�
						// ���� ������ ¦����
						CBay destinationbay=chromosome.getM_lstBay().get(bay_couple.get(k));
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-destinationbay.getLeft();
						// 3. central loop���� ������� �̵��Ÿ�
						// �������°Ÿ�
						zonetoeq+=destinationbay.getWidth();
						// ���� �̵�
						zonetoeq+=tobay.getLeft()-destinationbay.getLeft();
						// ������� �̵�
						zonetoeq+=toEQ.getCoordTop()+toEQ.getCoordLength()/2-tobay.getTop();
					}
					// ���̰� ���ʿ� �ִ� ���
					else {
						// 1. ���� ���񿡼� central loop���� ���� �Ÿ�
						// ���ۺ����� ¦���� ����
						CBay startbay=chromosome.getM_lstBay().get(bay_couple.get(i));
						// �ö󰡴� �Ÿ�
						eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
						// ������ �̵�
						eqtozone+=frombay.getLeft()-startbay.getLeft();
						// �������� �Ÿ�
						eqtozone+=startbay.getWidth();							
						// 2. central loop�� �̵��Ÿ�
						// ���� row�� �θ�
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// �ݴ��� row�� �θ�. ���̰� ���� ��ġ�Ǿ��ֱ� ������ index -1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);
						// central loop ������ �̵��Ÿ�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-startbay.getLeft();
						// central loop �Ʒ��� �̵��ϴ� �Ÿ�
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// central loop �������� �̵��ϴ� �Ÿ�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop �ö󰡴� �Ÿ�
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// central loop ���� ���̱��� �̵��ϴ� �Ÿ�
						zonetozone+=tobay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// 3. central loop���� ������� �̵��Ÿ�
						// ������� �ö󰡴� �̵��Ÿ�
						zonetoeq+=toEQ.getCoordTop()+toEQ.getCoordLength()/2-tobay.getTop();
					}
				}
			}
			// ���̰� �� ������ ��� 
			else {
				// �ð� ����
				if(fromEQ.getCoordTop()<toEQ.getCoordTop()) {
					eqtozone=Math.abs((fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-(toEQ.getCoordTop()+toEQ.getCoordLength()/2));
				}
				// �ð� �ݴ� ����  (¦ �ο�� �̵�)
				else {
					// ���̰� �Ʒ��ʿ� �ִ� ���
					if(frombay.isM_isBayUpperdirection()) {
						// 1. ���� ���񿡼� central loop���� ���� �Ÿ�
						// ���ۺ����� ¦ ���� ����
						CBay startbay=chromosome.getM_lstBay().get(bay_couple.get(i));
						// �������� �Ÿ�
						eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-frombay.getTop();
						// ¦���̷� �̵��Ÿ�
						eqtozone+=frombay.getLeft()-startbay.getLeft();
						// ¦���̿��� zone���� �ö󰡴� �Ÿ�
						eqtozone+=startbay.getWidth();
						// 2. central loop�� �̵��Ÿ�
						// ���� row�� �θ�
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// �ݴ��� row�� �θ�. ���̰� �Ʒ��� ��ġ�Ǿ��ֱ� ������ index +1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
						// central loop ���� �̵��Ÿ�
						zonetozone+=startbay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop ���� �̵��ϴ� �Ÿ�
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// central loop ���������� �̵��ϴ� �Ÿ�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop �������� �Ÿ�
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// central loop ���� ������ ¦���� �̵��ϴ� �Ÿ�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-tobay.getLeft();
						// 3. central loop���� ������� �̵��Ÿ�
						// �������°Ÿ�
						zonetoeq+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
					}
					// ���̰� ���ʿ� �ִ� ���
					else {
						// 1. ���� ���񿡼� central loop���� ���� �Ÿ�							
						// �������� �Ÿ�
						eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-frombay.getTop();					
						// 2. central loop�� �̵��Ÿ�
						// ���� row�� �θ�
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// �ݴ��� row�� �θ�. ���̰� ���� ��ġ�Ǿ��ֱ� ������ index -1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);
						// central loop ������ �̵��Ÿ�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-frombay.getLeft();
						// central loop �Ʒ��� �̵��ϴ� �Ÿ�
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());							
						// central loop �������� �̵��ϴ� �Ÿ�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop �ö󰡴� �Ÿ�
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// central loop ���� ���̱��� �̵��ϴ� �Ÿ�
						CBay destinationbay=chromosome.getM_lstBay().get(bay_couple.get(k));
						zonetozone+=destinationbay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());							
						// 3. central loop���� ������� �̵��Ÿ�
						// ������� �ö󰡴� �̵��Ÿ�
						zonetoeq+=destinationbay.getWidth();
						zonetoeq+=tobay.getLeft()-destinationbay.getLeft();
						zonetoeq+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
					}
				}
			}
		}
		
		// ���� ���̳� ��, �� ���� �����ϴ� ���
		else if (bay_couple.get(i)==k) {
			// ���̰� �Ʒ��� ��� (�� �Ʒ� �ο�)
			if(frombay.isM_isBayUpperdirection()) {
				// �ݴ� �ο�� �� �Ѿ�� ���
				if(i>k) {
					eqtozone+=fromEQ.getCoordTop()+fromEQ.getCoordLength()/2-frombay.getTop();
					eqtozone+=frombay.getLeft()-tobay.getLeft();
					eqtozone+=toEQ.getCoordTop()+toEQ.getCoordLength()/2-tobay.getTop();						
				}
				// �ݴ� �ο�� �Ѿ�� ���
				else {						
					// 1. ���� ���񿡼� central loop���� ���� �Ÿ�
					eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
					// 2. central loop�� �̵��Ÿ�
					// ���� row�� �θ�
					CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
					// �ݴ��� row�� �θ�. ���̰� �Ʒ��� ��ġ�Ǿ��ֱ� ������ index +1
					CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
					// central loop ���� �̵��Ÿ�
					zonetozone+=frombay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());
					// central loop ���� �̵��ϴ� �Ÿ�
					zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
					// central loop ���������� �̵��ϴ� �Ÿ�
					zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
					// central loop �������� �Ÿ�
					zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());						// 
					// central loop ���� ���̱��� �̵��ϴ� �Ÿ� 						
					zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-tobay.getLeft();
					// 3. central loop���� ������� �̵��Ÿ�						
					// ������� �̵�
					zonetoeq+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
				}
			}
			// ���̰� ���� �ִ� ��� (�Ʒ����� �ι�° �ο�)
			else {
				// �ݴ� �ο�� �� �Ѿ�� ���
				if(i<k) {
					eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
					eqtozone+=tobay.getLeft()-frombay.getLeft();
					eqtozone+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);						
				}
				// �ݴ� �ο�� �Ѿ�� ���
				else {
					// 1. ���� ���񿡼� central loop���� ���� �Ÿ�							
					// �������� �Ÿ�
					eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-(frombay.getTop()+frombay.getWidth());					
					// 2. central loop�� �̵��Ÿ�
					// ���� row�� �θ�
					CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
					// �ݴ��� row�� �θ�. ���̰� ���� ��ġ�Ǿ��ֱ� ������ index -1
					CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);
					// central loop ������ �̵��Ÿ�
					zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-frombay.getLeft();
					// central loop �Ʒ��� �̵��ϴ� �Ÿ�
					zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());							
					// central loop �������� �̵��ϴ� �Ÿ�
					zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
					// central loop �ö󰡴� �Ÿ�
					zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
					// central loop ���� ���̱��� �̵��ϴ� �Ÿ�						
					zonetozone+=tobay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());							
					// 3. central loop���� ������� �̵��Ÿ�
					// ������� �ö󰡴� �̵��Ÿ�
					zonetoeq+=(toEQ.getCoordTop()+toEQ.getCoordLength()/2)-tobay.getTop();	
				}				
			}
		}

		// ���� �ٸ� ���̿� �����ϴ� ���
		else {
			// 1. eqtozone
			// central loop���� ����ϴ� x
			double starting_x_central_loop;
			// ��� ������ ���̰� �Ʒ��ְ�, ��� ���� ���� ���ϴ� ���
			if(frombay.isM_isBayUpperdirection() && frombay.isM_isEQUpperDirection()) {
				eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
				starting_x_central_loop=frombay.getLeft();				
			}
			// ��� ������ ���̰� �Ʒ��ְ�, ��� ���� �Ʒ��� ���ϴ� ��� 
			else if(frombay.isM_isBayUpperdirection() && !frombay.isM_isEQUpperDirection()) {
				// ���ۺ����� ¦ ���� ����
				CBay startbay=chromosome.getM_lstBay().get(bay_couple.get(i));
				// �������� �Ÿ�
				eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-frombay.getTop();
				// ¦���̷� �̵��Ÿ�
				eqtozone+=frombay.getLeft()-startbay.getLeft();
				// ¦���̿��� zone���� �ö󰡴� �Ÿ�
				eqtozone+=startbay.getWidth();		
				starting_x_central_loop=startbay.getLeft();				
			}
			// ��� ������ ���̰� �����ְ�, ��� ���� ���� ���ϴ� ��� 
			else if(!frombay.isM_isBayUpperdirection() && frombay.isM_isEQUpperDirection()) {				
				// ���ۺ����� ¦���� ����
				CBay startbay=chromosome.getM_lstBay().get(bay_couple.get(i));
				// �ö󰡴� �Ÿ�
				eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
				// ������ �̵�
				eqtozone+=frombay.getLeft()-startbay.getLeft();
				// �������� �Ÿ�
				eqtozone+=startbay.getWidth();
				starting_x_central_loop=startbay.getLeft();
			}
			// ��� ������ ���̰� �����ְ�, ��� ���� �Ʒ��� ���ϴ� ���
			else {
				eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-frombay.getTop();
				starting_x_central_loop=frombay.getLeft();
			}			

			// 3. zonetoeq
			// central loop���� ����ϴ� x
			double ending_x_central_loop;			
			// ���� ������ ���̰� �Ʒ��ְ�, ���� ���� ���� ���ϴ� ���
			if(tobay.isM_isBayUpperdirection() && tobay.isM_isEQUpperDirection()) {
				CBay destinationbay=chromosome.getM_lstBay().get(bay_couple.get(k));
				zonetoeq+=destinationbay.getWidth();
				zonetoeq+=destinationbay.getLeft()-tobay.getLeft();
				zonetoeq+=(toEQ.getCoordTop()+toEQ.getCoordLength()/2)-tobay.getTop();
				ending_x_central_loop=destinationbay.getLeft();
			}
			// ���� ������ ���̰� �Ʒ��ְ�, ���� ���� �Ʒ��� ���ϴ� ���
			else if(tobay.isM_isBayUpperdirection() && !tobay.isM_isEQUpperDirection()) {
				zonetoeq+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
				ending_x_central_loop=tobay.getLeft();				
			}
			// ���� ������ ���̰� �����ְ�, ���� ���� ���� ���ϴ� ���
			else if(!tobay.isM_isBayUpperdirection() && tobay.isM_isEQUpperDirection()) {
				zonetoeq+=(toEQ.getCoordTop()+toEQ.getCoordLength()/2)-tobay.getTop();
				ending_x_central_loop=tobay.getLeft();
				
			}
			// ���� ������ ���̰� �����ְ�, ���� ���� �Ʒ��� ���ϴ� ���
			else {
				CBay destinationbay=chromosome.getM_lstBay().get(bay_couple.get(k));
				zonetoeq+=destinationbay.getWidth();
				zonetoeq+=tobay.getLeft()-destinationbay.getLeft();
				zonetoeq+=(tobay.getTop()-tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
				ending_x_central_loop=destinationbay.getLeft();
			}
			
			// 2. zonetozone
			// ���� ���� ����, �������� ��ġ
			if(tobay.getZone()==frombay.getZone()) {
				// �ݴ� ������ �̵����� ����
				if(i>k) {
					zonetozone=Math.abs(starting_x_central_loop-ending_x_central_loop);
				}
				// �ݴ� ������ �̵�
				else {
					// ���̰� �Ʒ��� ��ġ�� ���
					if(tobay.isM_isBayUpperdirection()) {
						// ���� row�� �θ�
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// �ݴ��� row�� �θ�. ���̰� �Ʒ��� ��ġ�Ǿ��ֱ� ������ index +1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
						zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), opposite_row.getLeft());
						// �ο찣 �̵��� ��� �պ��̱� ������ 2�踦 �ؼ� ������
						zonetozone+=(opposite_row.getTop()-(now_row.getTop()+now_row.getHeight()))*2;
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(), opposite_row.getLeft());
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-ending_x_central_loop;
					}
					// ���̰� ���� ��ġ�� ���
					else {
						// ���� row�� �θ�
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// �ݴ��� row�� �θ�. ���̰� ���� ��ġ�Ǿ��ֱ� ������ index -1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-starting_x_central_loop;
						// �ο찣 �̵��� ��� �պ��̱� ������ 2�踦 �ؼ� ������
						zonetozone+=(opposite_row.getTop()-(now_row.getTop()+now_row.getHeight()))*2;
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(), opposite_row.getLeft());
						zonetozone+=ending_x_central_loop-Math.min(now_row.getLeft(), opposite_row.getLeft());
					}
				}
			}
			// �ٸ� ���� ����, �������� ��ġ
			else {
				// ���� �ο츦 �θ�
				CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
				// ���� �ο츦 �θ�
				CZone target_row=dataSet.getM_lstZone().get(tobay.getZone());				
				// 1. ��� �ο��� ���̰� �Ʒ��� ��ġ�� ���
				if(frombay.isM_isBayUpperdirection()) {					
					// ���� �ο��� ���̰� �Ʒ��� ��ġ�� ���
					if(tobay.isM_isBayUpperdirection()) {						
						CZone opposite_row;						
						if(tobay.getZone()<frombay.getZone()) {
							// ���� �ο찡 ���ʿ� ��ġ�ϸ� ���� �ο��� ������ �ο� ȣ��
							opposite_row=dataSet.getM_lstZone().get(tobay.getZone()-1);	
						}
							// ��� �ο찡 ���ʿ� ��ġ�ϸ� ��� �ο��� ������ �ο� ȣ��
						else {
							opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
						}						
						// �ο� �������� �̵�
						zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), opposite_row.getLeft());
						// ���� �ο��� ������ �ο�� �̵�
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// ������ �ο��� ���������� �̵�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(), opposite_row.getLeft());
						// ���� �ο�� �̵�(���� �������� �̵�) 
						zonetozone+=opposite_row.getTop()-(target_row.getTop()+target_row.getHeight());
						// ���� �ο� �� ���� ���̱��� �̵�
						zonetozone+=(target_row.getLeft()+target_row.getWidth())-ending_x_central_loop;							
					}
					// ���� �ο��� ���̰� ���� ��ġ�� ���
					else {
						// ���� �ο찡 ��� �ο�� ���ֺ��� �ִ� ���
						if(tobay.getZone()==frombay.getZone()-1) {
							// �ο� �������� �̵�
							zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());
							// ���� �ο�� �̵�
							zonetozone+=target_row.getTop()-(now_row.getTop()+now_row.getHeight());
							// ���� �ο� �� ���� ���̱��� �̵�
							zonetozone+=ending_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());							
						}
						// ���� �ο찡 ��� �ο�� ���ֺ��� ���� �ʴ� ���
						else {
							// ���� �ο찡 �Ʒ��� ��ġ�� ���							
							if(tobay.getZone()>frombay.getZone()) {
								CZone opposite_fromrow=dataSet.getM_lstZone().get(frombay.getZone()-1);
								CZone opposite_torow=dataSet.getM_lstZone().get(tobay.getZone()+1);
								// �ο� �������� �̵�
								zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), opposite_fromrow.getLeft());
								// ��� �ο��� ���������� �̵�
								zonetozone+=opposite_fromrow.getTop()-(now_row.getTop()+now_row.getHeight());								
								// ��� �ο� �������� ���������� �̵�
								zonetozone+=Math.max(opposite_torow.getLeft()+opposite_torow.getWidth(), opposite_fromrow.getLeft()+opposite_fromrow.getWidth())-Math.min(now_row.getLeft(), opposite_fromrow.getLeft());
								// ��� �ο��� �������� ���� �ο��� ���� ������ �̵�
								zonetozone+=opposite_fromrow.getTop()-(opposite_torow.getTop()+opposite_torow.getHeight());
								// ���� �ο��� ������ �������� �̵�
								zonetozone+=Math.max(opposite_torow.getLeft()+opposite_torow.getWidth(), target_row.getLeft()+target_row.getWidth())-Math.min(opposite_torow.getLeft(), target_row.getLeft());
								// ���� �ο��� �������� ���� �ο�� �̵�
								zonetozone+=target_row.getTop()-(opposite_torow.getTop()+opposite_torow.getHeight());
								// ���� �ο쿡�� ���� ���̷� �̵�
								zonetozone+=ending_x_central_loop-Math.min(opposite_torow.getLeft(), target_row.getLeft());
							}
							// ���� �ο찡 ���� ��ġ�� ���
							else {
								// �ο� �������� �̵�
								zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());
								zonetozone+=target_row.getTop()-(now_row.getTop()+now_row.getHeight());
								zonetozone+=ending_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());								
							}
						}
					}
				}				
				// 2. ��� �ο��� ���̰� ���� ��ġ�� ���
				else {
					// ���� �ο��� ���̰� �Ʒ��� ��ġ�� ���
					if(tobay.isM_isBayUpperdirection()) {
						// ���� �ο찡 �Ʒ��� ��ġ�� ���							
						if(tobay.getZone()>frombay.getZone()) {
							// ��� �ο� �������� �̵�
							zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());
							// ���� �ο�� �̵� 
							zonetozone+=now_row.getTop()-(target_row.getTop()+target_row.getHeight());
							zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(), target_row.getLeft()+target_row.getWidth())-ending_x_central_loop;
						}
						// ���� �ο찡 ���� ��ġ�� ���
						else {							
							CZone opposite_fromrow=dataSet.getM_lstZone().get(frombay.getZone()+1);
							CZone opposite_torow=dataSet.getM_lstZone().get(tobay.getZone()-1);
							// �ο� �������� �̵�
							zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(), opposite_fromrow.getLeft()+opposite_fromrow.getWidth())-starting_x_central_loop;
							// ��� �ο��� ���������� �̵�
							zonetozone+=(opposite_fromrow.getTop()+opposite_fromrow.getHeight())-now_row.getTop();								
							// ��� �ο� ������ �ο��� �������� �̵�
							zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(), opposite_fromrow.getLeft()+opposite_fromrow.getWidth())-Math.min(opposite_torow.getLeft(), opposite_fromrow.getLeft());							
							// ��� �ο��� �������� ���� �ο��� ���� ������ �̵�
							zonetozone+=opposite_torow.getTop()-(opposite_fromrow.getTop()+opposite_fromrow.getHeight());
							// ���� �ο��� ������ �������� �̵�
							zonetozone+=Math.max(opposite_torow.getLeft()+opposite_torow.getWidth(), target_row.getLeft()+target_row.getWidth())-Math.min(opposite_torow.getLeft(), target_row.getLeft());
							// ���� �ο��� �������� ���� �ο�� �̵�
							zonetozone+=opposite_torow.getTop()-(target_row.getTop()+target_row.getHeight());
							// ���� �ο쿡�� ���� ���̷� �̵�
							zonetozone+=Math.max(opposite_torow.getLeft()+opposite_torow.getWidth(), target_row.getLeft()+target_row.getWidth())-ending_x_central_loop;
						}
					}
					// ���� �ο��� ���̰� ���� ��ġ�� ���
					else {
						CZone opposite_row;						
						if(tobay.getZone()>frombay.getZone()) {
							// ���� �ο찡 ���ʿ� ��ġ�ϸ� ��� �ο��� ������ �ο� ȣ��
							opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);	
						}
							// ��� �ο찡 ���ʿ� ��ġ�ϸ� ���� �ο��� ������ �ο� ȣ��
						else {
							opposite_row=dataSet.getM_lstZone().get(tobay.getZone()+1);
						}						
						// �ο� �������� �̵�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(), opposite_row.getLeft()+opposite_row.getWidth())-starting_x_central_loop;
						// ������ �ο�� �̵�
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// ������ �ο��� �������� �̵�
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(target_row.getLeft(), opposite_row.getLeft());
						// ���� �ο�� �̵� 
						zonetozone+=target_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// ���� �ο� �� ���� ���̱��� �̵�
						zonetozone+=ending_x_central_loop-Math.min(target_row.getLeft(), opposite_row.getLeft());		
					}					
				}
			}				
		}	
		return eqtozone+zonetozone+zonetoeq;
	}
	/**
	 * 
	 * @author : jwon.cho
	 * @version : 2010. 12. 08 ���� 5:22:27
	 * @date : 2010. 12. 08
	 * @param population
	 * @return
	 * @�����̷� :
	 * @Method ���� : EQ�� �ٸ� Room ���� �ݼ۷� ���
	 */
	private MicroOrganism calFitnessByDistance(MicroOrganism population, CRoom fromRoom)
	{
		EQ fromEQ;
		CRoom toRoom;
		String fromEQName, toEQName, toRoomName;
		String fromRoomName = fromRoom.getM_strName();
		String key;
		double freq;
		double distance;
		double totalDistance = 0;
		
		// ksn �̵� �Ÿ� ���� fitness�� �ٽ� ����ؾ���.
		// from EQ to Room
		ArrayList<EQ> eqListOfPopulation = population.getChromosomeByName();
		ArrayList<CRoom> roomList = resultSet.getM_bestRoomOrder().getChromosomeByName();
		Hashtable<String, Double> distanceToEachRoom = new Hashtable<String, Double>(); 
		
		for (int i = 0; i < eqListOfPopulation.size(); i++) {
			fromEQ = eqListOfPopulation.get(i);
			fromEQName = fromEQ.getDeviceName();
			
			for (int j = 0; j < roomList.size(); j++) {
				toRoom = roomList.get(j);
				toRoomName = toRoom.getM_strName();
				if(dataSet.CRITERION_IS_DISTANCE_2){ //H2 ����. room�� �ϳ�����. ���� ��� ���� �׳� ������ ��.
					if(toRoomName.equals(fromRoomName)){
						ArrayList<EQ> eqList = toRoom.getM_lstEQ();
						for (int k = 0; k < eqList.size(); k++) {
							toEQName = eqList.get(k).getDeviceName();
							freq = 0;
							key = fromEQName + "$" + toEQName;
							if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());
							distance = calculateDistanceInSameRoom2(fromEQ, eqList.get(k), fromRoom, freq);
							addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
							totalDistance += distance;
						}
					}
				} else {
					if(toRoomName.equals(fromRoomName)){
						// ���� ���� �� �ݼ� - ������ �ݼ۸� ����ؼ� 2�� ������
						// resultSet�� eqList�� ������ ��ġ�Ǿ� ����� ���̹Ƿ� �� ����Ʈ�� ����ϸ� �ȵ�.
						for (int k = 0; k < eqListOfPopulation.size(); k++) {
							freq = 0;
							toEQName = eqListOfPopulation.get(k).getDeviceName();
							key = fromEQName + "$" + toEQName;
							if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());
							distance = calculateDistanceFromEqToEq(fromEQ, eqListOfPopulation.get(k), freq);
							addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
							totalDistance += distance;
							
							freq = 0;
							key = toEQName + "$" + fromEQName;
							if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());
							distance = calculateDistanceFromEqToEq(eqListOfPopulation.get(k), fromEQ, freq);
							addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
							totalDistance += distance;
						}
					} else {
						if(!toRoom.isEqArranged()){
							// �ٸ� Room���� ������ �ݼ�
							freq = 0;
							distance = 0;
							key = fromEQName + "$" + toRoomName;
							
							if(null == dataSet.getM_htFromEQ_ToRoom().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromEQ_ToRoom().get(key).toString());
							
							distance += calculateDistanceFromEqToRoom(fromEQ, fromRoom, toRoom, freq);
							
							// �ٸ� Room���� ������ �ݼ�
							freq = 0;
							key = toRoomName + "$" + fromEQName;
							
							if(null == dataSet.getM_htFromRoom_ToEQ().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromRoom_ToEQ().get(key).toString());
							
							distance += calculateDistanceFromRoomToEq(toRoom, fromRoom, fromEQ, freq);
							addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
							totalDistance += distance;
						} else {
							ArrayList<EQ> eqList = toRoom.getM_lstEQ();
							for (int k = 0; k < eqList.size(); k++) {
								// �ٸ� Room���� ������ �ݼ�
								freq = 0;
								distance = 0.0d;
								toEQName = eqList.get(k).getDeviceName();
								key = fromEQName + "$" + toEQName;
								if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
								else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());	
								distance += calculateDistanceFromEqToEq(fromEQ, eqList.get(k), freq);
								
								// �ٸ� Room���� ������ �ݼ�
								freq = 0;
								key = toEQName + "$" + fromEQName;
								if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
								else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());	
								distance += calculateDistanceFromEqToEq(eqList.get(k), fromEQ, freq);
								
								addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
								
								totalDistance += distance;
							}
						}
					}
				}
			}
		}
		population.setDistanceToEachRoom(distanceToEachRoom);
		totalDistance = totalDistance / 1000000; //mm ������ km����
		totalDistance = totalDistance / 2; // ������ �ݼ۰� ������ �ݼ��� �ߺ����� ����Ͽ����Ƿ�, 2�� ������.
		population.setFitnessByDistance(totalDistance);
		
		double recalculatedFitness;
		for (int i = 0; i < roomList.size(); i++) {
			toRoom = roomList.get(i);
			toRoomName = toRoom.getM_strName();
			// fromRoom���� ������ �� ������ ����߰�,
			if(!fromRoomName.equals(toRoomName)){
				if(toRoom.isEqArranged()){
					// ���� ������ ���� fitness function ����
					recalculatedFitness = reCalFitnessByDistance(toRoom, population, fromRoom); 
					totalDistance += recalculatedFitness;
				}
			}
		}
		population.setIncrementalFitnessByDistance(totalDistance);
		return population;
	}

	private void addDistance(Hashtable<String, Double> distanceToEachRoom, String toRoomName, double distance) {
		double tmp = 0;
		if(distanceToEachRoom.containsKey(toRoomName)){
			tmp = distanceToEachRoom.get(toRoomName);
			distanceToEachRoom.remove(toRoomName);
		}
		distanceToEachRoom.put(toRoomName, (tmp + distance));
	}

	/**
	 * �� ��ġ�� �������� ���� �Ÿ��� ��������, �̹��� ��ġ�� �������� �Ÿ��� �̹��� ����� ������ ġȯ�Ͽ� ������
	 * @param fromRoom
	 * @param thisPopulation
	 * @param thisRoom
	 * @return
	 */
	private double reCalFitnessByDistance(CRoom fromRoom, MicroOrganism thisPopulation, CRoom thisRoom)
	{
		CRoom toRoom;
		String fromEqName, toEqName, toRoomName;
		String fromRoomName = fromRoom.getM_strName();
		double distance;
		double totalDistance = 0;
		ArrayList<CRoom> roomList = resultSet.getM_bestRoomOrder().getChromosomeByName();
		// �̹��� �� ��ġ�� ���������� �ݼ� �Ÿ� ����
		for (int i = 0; i < roomList.size(); i++) {
			toRoom = roomList.get(i);
			toRoomName = toRoom.getM_strName();
			if(toRoomName.equals(thisRoom.getM_strName())){ // �̹��� ��ġ�� �������� �Ÿ� ���� ������Ʈ.
				distance = thisPopulation.getDistanceToEachRoom().get(fromRoomName);
				totalDistance += distance;
			} else {
				totalDistance += fromRoom.getDistanceToEachRoom().get(toRoomName);
			}
		}
		return totalDistance;
	}
	
	/**
	 * @author jwon.cho
	 * @Method ����: EQ-Room �Ÿ� ����
	 * eq���� ���� ���⼺�� ���� ���� �� �ִ� ������� ������, ���� ���ĺ��� ���� room �߽ɱ����� �Ÿ��� ���Ѵ�.
	 * room�� �� ���� ���(�� �� �̻��� ������� ����) ���� ���� ������ ���� ������ ���ٰ� ���� ���.
	 * ���� zone ���ο� ���� ����ġ�� �ٸ��� ����.
	 */
	private double calculateDistanceFromEqToRoom(EQ fromEQ, CRoom fromRoom, CRoom toRoom, double freq) {
		double calculatedDistance = 0.0f;
		//subRoom�� �� �� �ִ� ��쿡 ���� ���� �߰�.. 3�� �̻��� ���� ���� ������ ���� jwon.cho
		double fromEqPortX, fromEqPortY; // �ݼ��� ������ ������ ��Ʈ ��ǥ
		double sideX, sideY; // �ݼ��� ������ ���񿡼� ������ ������ ���� ��ǥ
		CSubRoom toSubRoom_1, toSubRoom_2;
		double toRoomCenterX_1, toRoomCenterY_1, toRoomCenterX_2, toRoomCenterY_2; // �ݼ��� ���� ���� �߽� ��ǥ
		// ��Ʈ���� �����, ���濡�� �� �߽ɱ��� �Ÿ�, �հ�(����ġ �ο�)
		double distancePortToSide, distanceSideToRoomCenter_1, distanceSideToRoomCenter_2, distanceSum_1, distanceSum_2; 
boolean printSeletedEq = false;
//boolean printSeletedEq = fromEQ.isSelected();
if(printSeletedEq) System.out.println("****************fromEQ.getDeviceName(): " + fromEQ.getDeviceName() + " / toRoom: " + toRoom.getM_strName());
		//fromEQ Zone�� ��ġ ���⿡ ���� �ٸ��� ����ؾ� ��.
		CZone zone = getZone(fromEQ);
		if(zone.getType().equals("����")){
			// 1. Port ��ġ ���
			if(fromEQ.isM_isEQUpperDirection()) fromEqPortX = fromEQ.getCoordLeft() + fromEQ.getCoordWidth();
			else fromEqPortX = fromEQ.getCoordLeft();
			fromEqPortY = fromEQ.getCoordTop() + fromEQ.getCoordLength() / 2;
if(printSeletedEq) System.out.println("fromEQ.getCoordLeft(): " + fromEQ.getCoordLeft());				
if(printSeletedEq) System.out.println("fromEQ.getCoordWidth(): " + fromEQ.getCoordWidth());
if(printSeletedEq) System.out.println("fromEQ.getCoordTop(): " + fromEQ.getCoordTop());				
if(printSeletedEq) System.out.println("fromEQ.getCoordLength(): " + fromEQ.getCoordLength());
if(printSeletedEq) System.out.println("fromEqPortX: " + fromEqPortX);			
if(printSeletedEq) System.out.println("fromEqPortY: " + fromEqPortY);
			// 2. ���� ������ ���� ���� ��ġ ���
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//				if(fromEQ.isM_isEQUpperDirection()) sideY = fromRoom.getM_lstSubRoom().get(0).getTop() +fromRoom.getM_lstSubRoom().get(0).getHeight(); // ���� ����
//				else sideY = fromRoom.getM_lstSubRoom().get(0).getTop(); //�Ʒ��� ����
				if(fromEQ.isM_isEQUpperDirection()) sideY = zone.getTop() + zone.getHeight(); // ���� ����
				else sideY = zone.getTop(); //�Ʒ��� ����
			} else {
//				if(fromEQ.isM_isEQUpperDirection()) sideY = fromRoom.getM_lstSubRoom().get(0).getTop(); //�Ʒ��� ����
//				else sideY = fromRoom.getM_lstSubRoom().get(0).getTop() + fromRoom.getM_lstSubRoom().get(0).getHeight(); // ���� ����
				if(fromEQ.isM_isEQUpperDirection()) sideY = zone.getTop(); //�Ʒ��� ����
				else sideY = zone.getTop() + zone.getHeight(); // ���� ����
			}
			sideX = fromEqPortX;
if(printSeletedEq) System.out.println("sideX: " + sideX);			
if(printSeletedEq) System.out.println("sideY: " + sideY);
			// 3. eq - side �Ÿ� ���
			distancePortToSide = Math.abs(sideX - fromEqPortX) + Math.abs(sideY - fromEqPortY);
if(printSeletedEq) System.out.println("distancePortToSide: " + distancePortToSide);			
			
			///// toRoom�� �ϳ��� ��
			if(toRoom.getM_lstSubRoom().size() < 2){
				toSubRoom_1 = toRoom.getM_lstSubRoom().get(0);
				
				// 4-1. toRoom �߽� ��ġ ���
				toRoomCenterX_1 = toSubRoom_1.getLeft() + toSubRoom_1.getWidth() / 2;
				toRoomCenterY_1 = toSubRoom_1.getTop() + toSubRoom_1.getHeight() / 2;
if(printSeletedEq) System.out.println("toRoomCenterX_1: " + toRoomCenterX_1);				
if(printSeletedEq) System.out.println("toRoomCenterY_1: " + toRoomCenterY_1);
				// 5-1. side - room �Ÿ� ���
				distanceSideToRoomCenter_1 = Math.abs(sideX - toRoomCenterX_1) + Math.abs(sideY - toRoomCenterY_1);
if(printSeletedEq) System.out.println("distanceSideToRoomCenter_1: " + distanceSideToRoomCenter_1);				
				// 6-1. ��ü �Ÿ� ��ġ�� ����ġ ���ϱ�
				if(fromEQ.getZoneIndex() == toSubRoom_1.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� ��
					distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ROOM_PENALTY;
				else //// fromEq�� toRoom�� zone�� �ٸ� ��
					distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ZONE_PENALTY;
if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);				
				distanceSum_2 = 0;
if(printSeletedEq) System.out.println("distanceSum_2: " + distanceSum_2);				
				///// toRoom�� �� ���� ��(�� �� �̻��� ������� ����)	
			} else {
				toSubRoom_1 = toRoom.getM_lstSubRoom().get(0);
				toSubRoom_2 = toRoom.getM_lstSubRoom().get(1);
				
				// 4-2. toRoom �߽� ��ġ ���
				toRoomCenterX_1 = toSubRoom_1.getLeft() + toSubRoom_1.getWidth()/2;
				toRoomCenterY_1 = toSubRoom_1.getTop() + toSubRoom_1.getHeight()/2;
if(printSeletedEq) System.out.println("toRoomCenterX_1: " + toRoomCenterX_1);				
if(printSeletedEq) System.out.println("toRoomCenterY_1: " + toRoomCenterY_1);				
				toRoomCenterX_2 = toSubRoom_2.getLeft() + toSubRoom_2.getWidth()/2;
				toRoomCenterY_2 = toSubRoom_2.getTop() + toSubRoom_2.getHeight()/2;
if(printSeletedEq) System.out.println("toRoomCenterX_2: " + toRoomCenterX_2);				
if(printSeletedEq) System.out.println("toRoomCenterY_2: " + toRoomCenterY_2);
				// 5-2. side - room �Ÿ� ��� 
				distanceSideToRoomCenter_1 = Math.abs(sideX - toRoomCenterX_1) + Math.abs(sideY - toRoomCenterY_1);
				distanceSideToRoomCenter_2 = Math.abs(sideX - toRoomCenterX_2) + Math.abs(sideY - toRoomCenterY_2);
if(printSeletedEq) System.out.println("distanceSideToRoomCenter_1: " + distanceSideToRoomCenter_1);				
if(printSeletedEq) System.out.println("distanceSideToRoomCenter_2: " + distanceSideToRoomCenter_2);
				double area_1 = toSubRoom_1.getWidth() * toSubRoom_1.getHeight();
				double area_2 = toSubRoom_2.getWidth() * toSubRoom_2.getHeight();
if(printSeletedEq) System.out.println("area_1: " + area_1);
if(printSeletedEq) System.out.println("area_2: " + area_2);
				// 6-2. ��ü �Ÿ� ��ġ�� ����ġ ���ϱ�
				if(area_1 + area_2 > 0){
					if(fromEQ.getZoneIndex() == toSubRoom_1.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� �� 
						distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ROOM_PENALTY * (area_1 / (area_1 + area_2));
					else //// fromEq�� toRoom�� zone�� �ٸ� ��
						distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ZONE_PENALTY * (area_1 / (area_1 + area_2));
if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);					
					if(fromEQ.getZoneIndex() == toSubRoom_2.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� ��
						distanceSum_2 = (distancePortToSide + distanceSideToRoomCenter_2) * CDataSet.MICRO_ROOM_PENALTY * (area_2 / (area_1 + area_2));
					else //// fromEq�� toRoom�� zone�� �ٸ� ��
						distanceSum_2 = (distancePortToSide + distanceSideToRoomCenter_2) * CDataSet.MICRO_ZONE_PENALTY * (area_2 / (area_1 + area_2));
if(printSeletedEq) System.out.println("distanceSum_2: " + distanceSum_2);
				} else {
					// �� subRoom�� ũ�� ���� 0�̸� ��� �Ұ�.
					distanceSum_1 = 0;
					distanceSum_2 = 0;
				}
			}
		} else {
			// 1. Port ��ġ ���
			fromEqPortX = fromEQ.getCoordLeft() + fromEQ.getCoordWidth() / 2;
			if(fromEQ.isM_isEQUpperDirection()) fromEqPortY = fromEQ.getCoordTop() + fromEQ.getCoordLength();
			else fromEqPortY = fromEQ.getCoordTop();
			
			if(printSeletedEq) System.out.println("fromEQ.getCoordLeft(): " + fromEQ.getCoordLeft());				
			if(printSeletedEq) System.out.println("fromEQ.getCoordWidth(): " + fromEQ.getCoordWidth());
			if(printSeletedEq) System.out.println("fromEQ.getCoordTop(): " + fromEQ.getCoordTop());				
			if(printSeletedEq) System.out.println("fromEQ.getCoordLength(): " + fromEQ.getCoordLength());
			if(printSeletedEq) System.out.println("fromEqPortX: " + fromEqPortX);			
			if(printSeletedEq) System.out.println("fromEqPortY: " + fromEqPortY);
			
			// 2. ���� ������ ���� ���� ��ġ ���
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
				if(fromEQ.isM_isEQUpperDirection()) sideX = zone.getLeft(); //�������� ����
				else sideX = zone.getLeft() + zone.getWidth(); // ���������� ����
			} else {
				if(fromEQ.isM_isEQUpperDirection()) sideX = zone.getLeft() + zone.getWidth(); // ���������� ����
				else sideX = zone.getLeft(); //�������� ����
			}
			sideY = fromEqPortY;
			if(printSeletedEq) System.out.println("sideX: " + sideX);			
			if(printSeletedEq) System.out.println("sideY: " + sideY);
			// 3. eq - side �Ÿ� ���
			distancePortToSide = Math.abs(sideX-fromEqPortX) + Math.abs(sideY-fromEqPortY);
			if(printSeletedEq) System.out.println("distancePortToSide: " + distancePortToSide);
			
			///// toRoom�� �ϳ��� ��
			if(toRoom.getM_lstSubRoom().size() < 2){
				toSubRoom_1 = toRoom.getM_lstSubRoom().get(0);
				
				// 4-1. toRoom �߽� ��ġ ���
				toRoomCenterX_1 = toSubRoom_1.getLeft() + toSubRoom_1.getWidth() / 2;
				toRoomCenterY_1 = toSubRoom_1.getTop() + toSubRoom_1.getHeight() / 2;
				if(printSeletedEq) System.out.println("toRoomCenterX_1: " + toRoomCenterX_1);				
				if(printSeletedEq) System.out.println("toRoomCenterY_1: " + toRoomCenterY_1);
				// 5-1. side - room �Ÿ� ���
				distanceSideToRoomCenter_1 = Math.abs(sideX - toRoomCenterX_1) + Math.abs(sideY - toRoomCenterY_1);
				if(printSeletedEq) System.out.println("distanceSideToRoomCenter_1: " + distanceSideToRoomCenter_1);
				// 6-1. ��ü �Ÿ� ��ġ�� ����ġ ���ϱ�
				if(fromEQ.getZoneIndex() == toSubRoom_1.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� ��
					distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ROOM_PENALTY;
				else //// fromEq�� toRoom�� zone�� �ٸ� ��
					distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ZONE_PENALTY; 
				if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);
				distanceSum_2 = 0;
				///// toRoom�� �� ���� ��(�� �� �̻��� ������� ����)	
			} else {
				toSubRoom_1 = toRoom.getM_lstSubRoom().get(0);
				toSubRoom_2 = toRoom.getM_lstSubRoom().get(1);
				
				// 4-2. toRoom �߽� ��ġ ���
				toRoomCenterX_1 = toSubRoom_1.getLeft() + toSubRoom_1.getWidth()/2;
				toRoomCenterY_1 = toSubRoom_1.getTop() + toSubRoom_1.getHeight()/2;
				
				toRoomCenterX_2 = toSubRoom_2.getLeft() + toSubRoom_2.getWidth()/2;
				toRoomCenterY_2 = toSubRoom_2.getTop() + toSubRoom_2.getHeight()/2;
				
				// 5-2. side - room �Ÿ� ��� 
				distanceSideToRoomCenter_1 = Math.abs(sideX - toRoomCenterX_1) + Math.abs(sideY - toRoomCenterY_1);
				distanceSideToRoomCenter_2 = Math.abs(sideX - toRoomCenterX_2) + Math.abs(sideY - toRoomCenterY_2);
				
				double area_1 = toSubRoom_1.getWidth() * toSubRoom_1.getHeight();
				double area_2 = toSubRoom_2.getWidth() * toSubRoom_2.getHeight();
				// 6-2. ��ü �Ÿ� ��ġ�� ����ġ ���ϱ�
				if(area_1 + area_2 > 0){
					if(fromEQ.getZoneIndex() == toSubRoom_1.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� �� 
						distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ROOM_PENALTY * (area_1 / (area_1 + area_2));
					else //// fromEq�� toRoom�� zone�� �ٸ� ��
						distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ZONE_PENALTY * (area_1 / (area_1 + area_2));
					
					if(fromEQ.getZoneIndex() == toSubRoom_2.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� ��
						distanceSum_2 = (distancePortToSide + distanceSideToRoomCenter_2) * CDataSet.MICRO_ROOM_PENALTY * (area_2 / (area_1 + area_2));
					else //// fromEq�� toRoom�� zone�� �ٸ� ��
						distanceSum_2 = (distancePortToSide + distanceSideToRoomCenter_2) * CDataSet.MICRO_ZONE_PENALTY * (area_2 / (area_1 + area_2));
				} else {
					// �� subRoom�� ũ�� ���� 0�̸� ��� �Ұ�.
					distanceSum_1 = 0;
					distanceSum_2 = 0;
				}
			}
		}
		
		//7. �ݼ۷� ���ϱ�
		calculatedDistance = freq * (distanceSum_1 + distanceSum_2);
if(printSeletedEq) System.out.println("freq: " + freq);		
if(printSeletedEq) System.out.println("freq * (distanceSum_1 + distanceSum_2): " + freq * (distanceSum_1 + distanceSum_2));
		return calculatedDistance;
	}
	
	/**
	 * @author jwon.cho
	 * @Method ����: EQ-Room �Ÿ� ����
	 * eq���� ���� ���⼺�� ���� ���� �� �ִ� ������� ������, ���� ���ĺ��� ���� room �߽ɱ����� �Ÿ��� ���Ѵ�.
	 * room�� �� ���� ���(�� �� �̻��� ������� ����) ���� ���� ������ ���� ������ ���ٰ� ���� ���.
	 * ���� zone ���ο� ���� ����ġ�� �ٸ��� ����.
	 */
	private double calculateDistanceFromRoomToEq(CRoom fromRoom, CRoom toRoom, EQ toEQ, double freq) {
		double calculatedDistance = 0.0f;
		//subRoom�� �� �� �ִ� ��쿡 ���� ���� �߰�.. 3�� �̻��� ���� ���� ������ ���� jwon.cho
		
		double toEqPortX, toEqPortY; // �ݼ��� ���� ������ ��Ʈ ��ǥ
		double sideX, sideY; // �ݼ��� ���� ������ ���� ��ǥ
		CSubRoom fromSubRoom_1, fromSubRoom_2;
		double fromRoomCenterX_1, fromRoomCenterY_1, fromRoomCenterX_2, fromRoomCenterY_2; // �ݼ��� ������ ���� �߽� ��ǥ
		// �� �߽ɿ��� �����, ���濡�� ��Ʈ���� �Ÿ�, �հ�(����ġ �ο�)
		double distanceRoomCenterToSide_1, distanceRoomCenterToSide_2, distanceSideToPort, distanceSum_1, distanceSum_2; 
boolean printSeletedEq = false;
//boolean printSeletedEq = toEQ.isSelected();
if(printSeletedEq) System.out.println("****************toEQ.getDeviceName(): " + toEQ.getDeviceName() + " / fromRoom: " + fromRoom.getM_strName());
		//fromEQ Zone�� ��ġ ���⿡ ���� �ٸ��� ����ؾ� ��.
		CZone zone = getZone(toEQ);
		if(zone.getType().equals("����")){
			// 1. Port ��ġ ���
			if(toEQ.isM_isEQUpperDirection()) toEqPortX = toEQ.getCoordLeft() + toEQ.getCoordWidth();
			else toEqPortX = toEQ.getCoordLeft();
			toEqPortY = toEQ.getCoordTop() + toEQ.getCoordLength() / 2;			
if(printSeletedEq) System.out.println("toEqPortX: " + toEqPortX);			
if(printSeletedEq) System.out.println("toEqPortY: " + toEqPortY);
			// 2. ���� ������ ���� ���� ��ġ ���
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
				if(toEQ.isM_isEQUpperDirection()) sideY = zone.getTop(); // �Ʒ��� ��
				else sideY = zone.getTop() + zone.getHeight(); //���� ��
			} else {
				if(toEQ.isM_isEQUpperDirection()) sideY = zone.getTop() + zone.getHeight(); // ���� ��
				else sideY = zone.getTop(); //�Ʒ��� ��
			}
			sideX = toEqPortX;
if(printSeletedEq) System.out.println("sideX: " + sideX);			
if(printSeletedEq) System.out.println("sideY: " + sideY);
			// 3. eq - side �Ÿ� ���
			distanceSideToPort = Math.abs(sideX - toEqPortX) + Math.abs(sideY - toEqPortY);
if(printSeletedEq) System.out.println("distancePortToSide: " + distanceSideToPort);			
			
			///// fromRoom�� �ϳ��� ��
			if(fromRoom.getM_lstSubRoom().size() < 2){
				fromSubRoom_1 = fromRoom.getM_lstSubRoom().get(0);
				
				// 4-1. toRoom �߽� ��ġ ���
				fromRoomCenterX_1 = fromSubRoom_1.getLeft() + fromSubRoom_1.getWidth() / 2;
				fromRoomCenterY_1 = fromSubRoom_1.getTop() + fromSubRoom_1.getHeight() / 2;
if(printSeletedEq) System.out.println("fromRoomCenterX_1: " + fromRoomCenterX_1);				
if(printSeletedEq) System.out.println("fromRoomCenterY_1: " + fromRoomCenterY_1);
				// 5-1. side - room �Ÿ� ���
				distanceRoomCenterToSide_1 = Math.abs(sideX - fromRoomCenterX_1) + Math.abs(sideY - fromRoomCenterY_1);
if(printSeletedEq) System.out.println("distanceSideToRoomCenter_1: " + distanceRoomCenterToSide_1);				
				// 6-1. ��ü �Ÿ� ��ġ�� ����ġ ���ϱ�
				if(toEQ.getZoneIndex() == fromSubRoom_1.getM_nZoneIndex()) //// toEq�� fromRoom�� zone�� ���� ��
					distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY;
				else //// fromEq�� toRoom�� zone�� �ٸ� ��
					distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY;
if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);				
				distanceSum_2 = 0;
if(printSeletedEq) System.out.println("distanceSum_2: " + distanceSum_2);				
				///// fromRoom�� �� ���� ��(�� �� �̻��� ������� ����)	
			} else {
				fromSubRoom_1 = fromRoom.getM_lstSubRoom().get(0);
				fromSubRoom_2 = fromRoom.getM_lstSubRoom().get(1);
				
				// 4-2. toRoom �߽� ��ġ ���
				fromRoomCenterX_1 = fromSubRoom_1.getLeft() + fromSubRoom_1.getWidth()/2;
				fromRoomCenterY_1 = fromSubRoom_1.getTop() + fromSubRoom_1.getHeight()/2;
if(printSeletedEq) System.out.println("fromRoomCenterX_1: " + fromRoomCenterX_1);				
if(printSeletedEq) System.out.println("fromRoomCenterY_1: " + fromRoomCenterY_1);				
				fromRoomCenterX_2 = fromSubRoom_2.getLeft() + fromSubRoom_2.getWidth()/2;
				fromRoomCenterY_2 = fromSubRoom_2.getTop() + fromSubRoom_2.getHeight()/2;
if(printSeletedEq) System.out.println("fromRoomCenterX_2: " + fromRoomCenterX_2);				
if(printSeletedEq) System.out.println("fromRoomCenterY_2: " + fromRoomCenterY_2);
				// 5-2. side - room �Ÿ� ��� 
				distanceRoomCenterToSide_1 = Math.abs(sideX - fromRoomCenterX_1) + Math.abs(sideY - fromRoomCenterY_1);
				distanceRoomCenterToSide_2 = Math.abs(sideX - fromRoomCenterX_2) + Math.abs(sideY - fromRoomCenterY_2);
if(printSeletedEq) System.out.println("distanceRoomCenterToSide_1: " + distanceRoomCenterToSide_1);				
if(printSeletedEq) System.out.println("distanceRoomCenterToSide_2: " + distanceRoomCenterToSide_2);
				double area_1 = fromSubRoom_1.getWidth() * fromSubRoom_1.getHeight();
				double area_2 = fromSubRoom_2.getWidth() * fromSubRoom_2.getHeight();
if(printSeletedEq) System.out.println("area_1: " + area_1);
if(printSeletedEq) System.out.println("area_2: " + area_2);
				// 6-2. ��ü �Ÿ� ��ġ�� ����ġ ���ϱ�
				if(area_1 + area_2 > 0){
					if(toEQ.getZoneIndex() == fromSubRoom_1.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� �� 
						distanceSum_1 = (distanceSideToPort + distanceRoomCenterToSide_1) * CDataSet.MICRO_ROOM_PENALTY * (area_1 / (area_1 + area_2));
					else //// fromEq�� toRoom�� zone�� �ٸ� ��
						distanceSum_1 = (distanceSideToPort + distanceRoomCenterToSide_1) * CDataSet.MICRO_ZONE_PENALTY * (area_1 / (area_1 + area_2));
if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);					
					if(toEQ.getZoneIndex() == fromSubRoom_2.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� ��
						distanceSum_2 = (distanceSideToPort + distanceRoomCenterToSide_2) * CDataSet.MICRO_ROOM_PENALTY * (area_2 / (area_1 + area_2));
					else //// fromEq�� toRoom�� zone�� �ٸ� ��
						distanceSum_2 = (distanceSideToPort + distanceRoomCenterToSide_2) * CDataSet.MICRO_ZONE_PENALTY * (area_2 / (area_1 + area_2));
if(printSeletedEq) System.out.println("distanceSideToPort: " + distanceSum_2);
				} else {
					// �� subRoom�� ũ�� ���� 0�̸� ��� �Ұ�.
					distanceSum_1 = 0;
					distanceSum_2 = 0;
				}
			}
		} else {
			// 1. Port ��ġ ���
			toEqPortX = toEQ.getCoordLeft() + toEQ.getCoordWidth() / 2;
			if(toEQ.isM_isEQUpperDirection()) toEqPortY = toEQ.getCoordTop() + toEQ.getCoordLength();
			else toEqPortY = toEQ.getCoordTop();
			
			// 2. ���� ������ ���� ���� ��ġ ���
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
				if(toEQ.isM_isEQUpperDirection()) sideX = zone.getLeft() + zone.getWidth(); // ���������� ��
				else sideX = zone.getLeft(); //�������� ��
			} else {
				if(toEQ.isM_isEQUpperDirection()) sideX = zone.getLeft(); //�������� ��
				else sideX = zone.getLeft() + zone.getWidth(); // ���������� ��
			}
			sideY = toEqPortY;
			
			// 3. eq - side �Ÿ� ���
			distanceSideToPort = Math.abs(sideX - toEqPortX) + Math.abs(sideY - toEqPortY);
			
			///// fromRoom�� �ϳ��� ��
			if(fromRoom.getM_lstSubRoom().size() < 2){
				fromSubRoom_1 = fromRoom.getM_lstSubRoom().get(0);
				
				// 4-1. fromRoom �߽� ��ġ ���
				fromRoomCenterX_1 = fromSubRoom_1.getLeft() + fromSubRoom_1.getWidth() / 2;
				fromRoomCenterY_1 = fromSubRoom_1.getTop() + fromSubRoom_1.getHeight() / 2;
				
				// 5-1. side - room �Ÿ� ���
				distanceRoomCenterToSide_1 = Math.abs(sideX - fromRoomCenterX_1) + Math.abs(sideY - fromRoomCenterY_1);
				
				// 6-1. ��ü �Ÿ� ��ġ�� ����ġ ���ϱ�
				if(toEQ.getZoneIndex() == fromSubRoom_1.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� ��
					distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY;
				else //// fromEq�� toRoom�� zone�� �ٸ� ��
					distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY; 
				distanceSum_2 = 0;
				///// fromRoom �� �� ���� ��(�� �� �̻��� ������� ����)	
			} else {
				fromSubRoom_1 = fromRoom.getM_lstSubRoom().get(0);
				fromSubRoom_2 = fromRoom.getM_lstSubRoom().get(1);
				
				// 4-2. fromRoom �߽� ��ġ ���
				fromRoomCenterX_1 = fromSubRoom_1.getLeft() + fromSubRoom_1.getWidth()/2;
				fromRoomCenterY_1 = fromSubRoom_1.getTop() + fromSubRoom_1.getHeight()/2;
				
				fromRoomCenterX_2 = fromSubRoom_2.getLeft() + fromSubRoom_2.getWidth()/2;
				fromRoomCenterY_2 = fromSubRoom_2.getTop() + fromSubRoom_2.getHeight()/2;
				
				// 5-2. side - room �Ÿ� ��� 
				distanceRoomCenterToSide_1 = Math.abs(sideX - fromRoomCenterX_1) + Math.abs(sideY - fromRoomCenterY_1);
				distanceRoomCenterToSide_2 = Math.abs(sideX - fromRoomCenterX_2) + Math.abs(sideY - fromRoomCenterY_2);
				
				double area_1 = fromSubRoom_1.getWidth() * fromSubRoom_1.getHeight();
				double area_2 = fromSubRoom_2.getWidth() * fromSubRoom_2.getHeight();
				// 6-2. ��ü �Ÿ� ��ġ�� ����ġ ���ϱ�
				if(area_1 + area_2 > 0){
					if(toEQ.getZoneIndex() == fromSubRoom_1.getM_nZoneIndex()) //// toEq�� fromRoom�� zone�� ���� �� 
						distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY * (area_1 / (area_1 + area_2));
					else //// fromEq�� toRoom�� zone�� �ٸ� ��
						distanceSum_1 = (distanceRoomCenterToSide_2 + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY * (area_1 / (area_1 + area_2));
					
					if(toEQ.getZoneIndex() == fromSubRoom_2.getM_nZoneIndex()) //// fromEq�� toRoom�� zone�� ���� ��
						distanceSum_2 = (distanceRoomCenterToSide_2 + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY * (area_2 / (area_1 + area_2));
					else //// fromEq�� toRoom�� zone�� �ٸ� ��
						distanceSum_2 = (distanceRoomCenterToSide_2 + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY * (area_2 / (area_1 + area_2));
				} else {
					// �� subRoom�� ũ�� ���� 0�̸� ��� �Ұ�.
					distanceSum_1 = 0;
					distanceSum_2 = 0;
				}
			}
		}
		//7. �ݼ۷� ���ϱ�
		calculatedDistance = freq * (distanceSum_1 + distanceSum_2);
if(printSeletedEq) System.out.println("freq: " + freq);		
if(printSeletedEq) System.out.println("freq * (distanceSum_1 + distanceSum_2): " + freq * (distanceSum_1 + distanceSum_2));
		return calculatedDistance;
	}
	/**
	 * @author jwon.cho
	 * @param fromEQ
	 * @param toRoom
	 * @param totalDistance
	 * @param freq
	 * @return
	 * @Method ����: ���� Room ������ EQ-EQ �Ÿ� ����
	 * zone�� ���� ��� Room�� ���� �� �� ����� ������ ������ ���� �Ǵ� �߾���θ� ���� �ٽ� ���ٰ� ����
	 * ���⼺�� ����ϵ��� �����ؾ� �մϴ�!!!!!!!!!!!!!
	 */
	public double calculateDistanceFromEqToEq(EQ fromEQ, EQ toEQ, double freq)
	{
		if(fromEQ.getDeviceName().equals(toEQ.getDeviceName())) 
			return 0.0d; // �ڽſ� ���ؼ��� ������� ����.
		double calculatedDistance;
		
		double fromEqPortX, fromEqPortY; // �ݼ��� ������ ������ ��Ʈ ��ǥ
		double fromSideX, fromSideY; // �ݼ��� ������ ������ ���� ��ǥ
		double toEqPortX, toEqPortY; // �ݼ��� ���� ������ ��Ʈ ��ǥ
		double toSideX, toSideY; // �ݼ��� ������ ������ ���� ��ǥ
		double distancePortToSide, distanceSideToSide, distanceSideToPort; // fromEq-fromSide, fromSide-toSide, toSide-toEq
		boolean isFromToDirect = false; //�ݼ��� ������ ���� �ʿ� ���� �ٷ� ���� ������ ���� ���� �� �� �ֳ�. 
		
		CZone fromZone = getZone(fromEQ);
//boolean printSeletedEq = fromEQ.isSelected();
		boolean printSeletedEq = false;
		if(printSeletedEq)
			System.out.println("fromEQ: " + fromEQ.getDeviceName() + " / toEQ: " + toEQ.getDeviceName());
		
		if(fromZone.getType().equals("����"))
		{
			// 1. fromEQPort ��ġ ���
			if(fromEQ.isM_isEQUpperDirection()) fromEqPortX = fromEQ.getCoordLeft() + fromEQ.getCoordWidth();
			else fromEqPortX = fromEQ.getCoordLeft();
			
			fromEqPortY = fromEQ.getCoordTop() + fromEQ.getCoordLength() / 2;
			
			// 2. ���� ������ ���� ���� ��ġ ���
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE)
			{
				if(fromEQ.isM_isEQUpperDirection()) fromSideY = fromZone.getTop() + fromZone.getHeight(); // ���� ����
				else fromSideY = fromZone.getTop(); //�Ʒ��� ����
			}
			else 
			{
				if(fromEQ.isM_isEQUpperDirection()) fromSideY = fromZone.getTop(); //�Ʒ��� ����
				else fromSideY = fromZone.getTop() + fromZone.getHeight(); // ���� ����
			}
			fromSideX = fromEqPortX;
			
		} 
		else 
		{
			// 1. fromEQPort ��ġ ���
			fromEqPortX = fromEQ.getCoordLeft() + fromEQ.getCoordWidth() / 2;
			
			if(fromEQ.isM_isEQUpperDirection()) fromEqPortY = fromEQ.getCoordTop() + fromEQ.getCoordLength();
			else fromEqPortY = fromEQ.getCoordTop();
			
			// 2. ���� ������ ���� ���� ��ġ ���
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
				if(fromEQ.isM_isEQUpperDirection()) fromSideX = fromZone.getLeft(); //�������� ����
				else fromSideX = fromZone.getLeft() + fromZone.getWidth(); // ���������� ����
			} else {
				if(fromEQ.isM_isEQUpperDirection()) fromSideX = fromZone.getLeft() + fromZone.getWidth(); // ���������� ����
				else fromSideX = fromZone.getLeft(); //�������� ����
			}
			fromSideY = fromEqPortY;
		}
		// 3. fromEq - fromSide �Ÿ� ���
		distancePortToSide = Math.abs(fromSideX - fromEqPortX) + Math.abs(fromSideY - fromEqPortY);
		
		if(printSeletedEq) 
			System.out.println("fromSideX: " + fromSideX + " / fromSideY: " + fromSideY);		
		if(printSeletedEq) 
			System.out.println("fromEqPortX: " + fromEqPortX + " / fromEqPortY: " + fromEqPortY);
		
		CZone toZone = getZone(toEQ);
		
		if(toZone.getType().equals("����"))
		{
			// 1. toEQPort ��ġ ���	
			if(toEQ.isM_isEQUpperDirection()) toEqPortX = toEQ.getCoordLeft() + toEQ.getCoordWidth();
			else toEqPortX = toEQ.getCoordLeft();
			
			toEqPortY = toEQ.getCoordTop() + toEQ.getCoordLength() / 2;
			
			// 2. ���� ������ ���� ���� ��ġ ���
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE)
			{
				if(toEQ.isM_isEQUpperDirection()) toSideY = toZone.getTop(); //�Ʒ��� ����
				else toSideY = toZone.getTop() + toZone.getHeight(); // ���� ����
				
			} 
			else 
			{
				if(toEQ.isM_isEQUpperDirection()) toSideY = toZone.getTop() + toZone.getHeight(); // ���� ����
				else toSideY = toZone.getTop(); //�Ʒ��� ����
				
			}
			toSideX = toEqPortX;
			
			if(fromEqPortX == toEqPortX)
			{
				if((fromEqPortY < toEqPortY && toEqPortY < fromSideY) || (fromEqPortY > toEqPortY && toEqPortY > fromSideY))
				{
					if(printSeletedEq) 
						System.out.println("���̷�Ʈ �ݼ�");
					isFromToDirect = true;
				}
			}
		}
		else 
		{
			// 1. toEQPort ��ġ ���
			toEqPortX = toEQ.getCoordLeft() + toEQ.getCoordWidth() / 2;
			
			if(toEQ.isM_isEQUpperDirection()) toEqPortY = toEQ.getCoordTop() + toEQ.getCoordLength();
			else toEqPortY = toEQ.getCoordTop();
			
			// 2. ���� ������ ���� ���� ��ġ ���
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE)
			{
				if(toEQ.isM_isEQUpperDirection()) toSideX = toZone.getLeft() + toZone.getWidth(); // ���������� ����
				else toSideX = toZone.getLeft(); //�������� ����
				
			}
			else
			{
				if(toEQ.isM_isEQUpperDirection()) toSideX = toZone.getLeft(); //�������� ����
				else toSideX = toZone.getLeft() + toZone.getWidth(); // ���������� ����
			}
			toSideY = toEqPortY;
			
			if(fromEqPortY == toEqPortY)
			{
				if((fromEqPortX < toEqPortX && toEqPortX < fromSideX) || (fromEqPortX > toEqPortX && toEqPortX > fromSideX))
				{
					if(printSeletedEq) 
						System.out.println("���̷�Ʈ �ݼ�");
					isFromToDirect = true;
				}
			}
		}
		// 3. toSide - toEq �Ÿ� ���
		distanceSideToPort = Math.abs(toSideX - toEqPortX) + Math.abs(toSideY - toEqPortY);
		
		if(printSeletedEq) System.out.println("toSideX: " + toSideX + " / toSideY: " + toSideY);		
		if(printSeletedEq) System.out.println("toEqPortX: " + toEqPortX + " / toEqPortY: " + toEqPortY);

		// 4. fromSide - toSide �Ÿ� ���
		distanceSideToSide = Math.abs(toSideX - fromSideX) + Math.abs(toSideY - fromSideY);
		
		if(isFromToDirect)
		{
			calculatedDistance = freq * (Math.abs(fromEqPortX - toEqPortX) + Math.abs(fromEqPortY-toEqPortY)) * CDataSet.MICRO_ROOM_PENALTY;
		} 
		else 
		{
			if(fromZone.getZoneIndex() == toZone.getZoneIndex())
			{			
				calculatedDistance = freq * (distancePortToSide + distanceSideToSide + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY;
			} 
			else 
			{
				calculatedDistance = freq * (distancePortToSide + distanceSideToSide + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY;
			}
		}
		if(printSeletedEq) System.out.println("distancePortToSide: " + distancePortToSide + " / distanceSideToSide: " + distanceSideToSide
				+ " / distanceSideToPort: " + distanceSideToPort);
		if(printSeletedEq) System.out.println("freq: " + freq);
			return calculatedDistance;
	}
	/**
	 * @author jwon.cho
	 * @param fromEQ
	 * @param toRoom
	 * @param totalDistance
	 * @param freq
	 * @return
	 * @Method ����: ���� Room ������ EQ-EQ �Ÿ� ����
	 * zone�� ���� ��� Room�� ���� �� �� ����� ������ ������ ���� �Ǵ� �߾���θ� ���� �ٽ� ���ٰ� ����
	 * ���⼺�� ����ϵ��� �����ؾ� �մϴ�!!!!!!!!!!!!!
	 */
//	private double calculateDistanceInSameRoom333(EQ fromEQ, EQ toEQ, CRoom room, double totalDistance, double freq) {		
//		double x1 = fromEQ.getCoordLeft() + fromEQ.getCoordWidth()/2;
//		double y1 = fromEQ.getCoordTop() + fromEQ.getCoordLength()/2;
//		int fromZoneIndex = fromEQ.getZoneIndex();
//		
//		double x2 = toEQ.getCoordLeft() + toEQ.getCoordWidth()/2;
//		double y2 = toEQ.getCoordTop() + toEQ.getCoordLength()/2;
//		int toZoneIndex = toEQ.getZoneIndex();
//
//		if(fromZoneIndex == toZoneIndex){
//			//zone�� �����Ƿ� fromRoomLeft�� toRoomLeft�� ���� ��������, �ǹ̻� ����
//			double fromRoomLeft = room.getM_lstSubRoom().get(0).getLeft();
//			double fromRoomRight = room.getM_lstSubRoom().get(0).getLeft() + room.getM_lstSubRoom().get(0).getWidth();
//			
//			double toRoomLeft = room.getM_lstSubRoom().get(0).getLeft();
//			double toRoomRight = room.getM_lstSubRoom().get(0).getLeft() + room.getM_lstSubRoom().get(0).getWidth();
//			
//			double distanceX = Math.min(Math.abs(x1-fromRoomLeft) + Math.abs(toRoomLeft-x2), Math.abs(x1-fromRoomRight) + Math.abs(toRoomRight-x2));
//
//			// ������ �ݼ۷��� ������ �ݼ۷��� ����ؾ� �ϴµ�, �׳� ���⼭ 2�� �����־� �����
//			totalDistance += freq * (distanceX + Math.abs(y1 - y2)) * CDataSet.MICRO_ROOM_PENALTY * 2;
//		} else {
//			totalDistance += freq * (Math.abs(x1 - x2) + Math.abs(y1 - y2)) * CDataSet.MICRO_ZONE_PENALTY * 2; // 
//		}
//		return totalDistance;
//	}
	/**
	 * @author jwon.cho
	 * @param fromEQ
	 * @param toRoom
	 * @param totalDistance
	 * @param freq
	 * @return
	 * @Method ����: ���� Room ������ EQ-EQ �Ÿ� ����
	 * H3 ���� ���
	 * ���� �߽� ����ϵ� �ٱ����� ������ ���� �ʰ�, �ٷ� �����Ÿ� ���.
	 */
	private double calculateDistanceInSameRoom2(EQ fromEQ, EQ toEQ, CRoom room, double freq) {
		double calculatedDistance = 0.0f;
		double x1 = fromEQ.getCoordLeft() + fromEQ.getCoordWidth()/2;
		double y1 = fromEQ.getCoordTop() + fromEQ.getCoordLength()/2;
		int fromZoneIndex = fromEQ.getZoneIndex();
		
		double x2 = toEQ.getCoordLeft() + toEQ.getCoordWidth()/2;
		double y2 = toEQ.getCoordTop() + toEQ.getCoordLength()/2;
		int toZoneIndex = toEQ.getZoneIndex();
		double distance1;
		double distance2;
		if(fromZoneIndex == toZoneIndex){
			//zone�� �����Ƿ� fromRoomLeft�� toRoomLeft�� ���� ��������, �ǹ̻� ����
//			double fromRoomLeft = room.getM_lstSubRoom().get(0).getLeft();
//			double fromRoomRight = room.getM_lstSubRoom().get(0).getLeft() + room.getM_lstSubRoom().get(0).getWidth();
//			
//			double toRoomLeft = room.getM_lstSubRoom().get(0).getLeft();
//			double toRoomRight = room.getM_lstSubRoom().get(0).getLeft() + room.getM_lstSubRoom().get(0).getWidth();
//			
//			double distanceX = Math.min(Math.abs(x1-fromRoomLeft) + Math.abs(toRoomLeft-x2), Math.abs(x1-fromRoomRight) + Math.abs(toRoomRight-x2));
//			
//			totalDistance  += freq * (distanceX+Math.abs(y1-y2) * CDataSet.MICRO_ROOM_PENALTY); 
			calculatedDistance = freq * (Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ROOM_PENALTY; //
			distance1 =(Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ROOM_PENALTY;
			distance2 = freq * (Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ROOM_PENALTY; //
		} else {
			calculatedDistance = freq * (Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ZONE_PENALTY; //
			distance1 =(Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ZONE_PENALTY;
			distance2 = freq * (Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ZONE_PENALTY; //
		}
//		System.out.println("fromEQ.getDeviceName():" + fromEQ.getDeviceName() + " / toEQ.getDeviceName():" + toEQ.getDeviceName() + " / distance1:" + distance1 + " / distance:" + distance2);
		return calculatedDistance;
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 16 ���� 9:18:31
	 * @date : 2010. 07. 16
	 * @param population
	 * @return
	 * @�����̷� :
	 * @Method ���� : ���� ������ ���� �ؿ� ��ȭ�� ��
	 */
	private MicroOrganism mutate(MicroOrganism population)
	{
		ArrayList<EQ> chromosomeByName = new ArrayList<EQ>();
		ArrayList<Integer> chromosomeByInt = new ArrayList<Integer>();

		// ������ �߻��Ͽ� �� ���� �Ķ����(0.07)���� ���� ���
		// size ���� ������ �� �� �߻����� �� �� ��ġ�� ���� ��ȯ
		int size = population.getChromosomeByInt().size();
		for (int i = 0; i < 2; i++)
		{
			if (Math.abs(Math.random()) < CDataSet.MICRO_MUTATE_RATIO)
			{
				int chooseLoc1 = new Random().nextInt(size);
				int chooseLoc2 = new Random().nextInt(size);
				int Num1 = population.getChromosomeByInt().get(chooseLoc1);
				int Num2 = population.getChromosomeByInt().get(chooseLoc2);

				int temp = Num1;
				population.getChromosomeByInt().set(chooseLoc1, Num2);
				population.getChromosomeByInt().set(chooseLoc2, temp);

				chromosomeByInt = population.getChromosomeByInt();
				chromosomeByName = population.getChromosomeByName();

				// ���� ��ġ ������ ���� ���·� �Ǿ� �ִ� ���� �̸����� Mapping �ϴ� �۾�.
				chromosomeByName = eqMappingToName(chromosomeByInt, chromosomeByName);
				population.setChromosomeByName(chromosomeByName);

				// // ��ȯ �� ���� feasible ���� ������ ����

				// boolean isFeasibleChromosome = Chromo1.getFeasible(room);
				// if (!isFeasibleChromosome) repair(Chromo1);
			}
		}
		return population;
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 ���� 9:28:39
	 * @date : 2010. 07. 19
	 * @�����̷� :
	 * @Method ���� : ������ ����� ��ŭ �ݺ��� ���Ŀ� ����� �����.
	 */
	void finish()
	{
		elitism();
//		swap();
		if(CDataSet.DEBUG){
//			if(true){
			for (int i = 0; i < lstPopulations.size(); i++)
			{
	//			System.out.print("\n"+i + "th: " + lstPopulations.get(i).getFitnessByArea());
				System.out.print("\n"+i + "th: " + lstPopulations.get(i).getFitnessByDistance());
				for (int j = 0; j < lstPopulations.get(i).getChromosomeByInt().size(); j++)
				{
					System.out.print(" " + lstPopulations.get(i).getChromosomeByInt().get(j));
				}
			}
			System.out.println();
		}
		// resultSet�� ��� ����
		
		resultSet.getM_lstBayOrder().addAll(lstPopulations.get(0).getM_lstBay());
		// �� eq�� ����� zone ������Ʈ
		updateDataSetEq(lstPopulations.get(0).getM_lstBay());
	}
	
	private void updateDataSetEq(ArrayList<CBay> bay) {
		for(int i=0;i<bay.size();i++){
			CBay cBay = bay.get(i);
			for(int j=0;j<cBay.getListEq().size();j++){
				cBay.getListEq().get(j).setZoneIndex(cBay.getZone());
				EQ eq = dataSet.getM_htEq().get(cBay.getListEq().get(j).getDeviceName());				
				eq.setZoneIndex(cBay.getZone());
//				eq.setM_isBayUpperdirection(cBay.isM_isBayUpperdirection());
//				eq.setM_isEQUpperDirection(cBay.isM_isEQUpperDirection());
			}
		}
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 ���� 9:28:39
	 * @date : 2010. 07. 19
	 * @�����̷� :
	 * @Method ���� : ������ ����� ��ŭ �ݺ��� ���Ŀ� ����� �����.
	 */
	void finishArea()
	{
		elitism();
//		swap();
		if(CDataSet.DEBUG){
			for (int i = 0; i < lstPopulations.size(); i++)
			{
	//			System.out.print("\n"+i + "th: " + lstPopulations.get(i).getFitnessByArea());
				System.out.print("\n"+i + "th: " + lstPopulations.get(i).getFitnessByArea());
				for (int j = 0; j < lstPopulations.get(i).getChromosomeByInt().size(); j++)
				{
					System.out.print(" " + lstPopulations.get(i).getChromosomeByInt().get(j));
				}
			}
			System.out.println();
		}
		// resultSet�� ��� ����
		resultSet.getM_lstBayOrder().addAll(lstPopulations.get(0).getM_lstBay());
		// �� eq�� ����� zone ������Ʈ
		updateDataSetEq(lstPopulations.get(0).getM_lstBay());
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 ���� 1:39:29
	 * @date : 2010. 07. 15
	 * @return
	 * @�����̷� :
	 * @Method ���� : ���������� �����ϱ� ���� �ʿ��� �θ� ����, Roulette Wheel Selection�� ������ ���Ŀ� ���� ����.
	 */
	private int getRandomPopulation()
	{
		float sumOfFitnessReciprocal = 0; // fitness �Լ��� ������ ���
		for (int i = 0; i < lstPopulations.size(); i++) {
			if(lstPopulations.get(i).getIncrementalFitnessByDistance()!=0)
				sumOfFitnessReciprocal += (float)1/lstPopulations.get(i).getIncrementalFitnessByDistance();
		}
		Random r = new Random();
		float point = r.nextFloat() * sumOfFitnessReciprocal;
		float sum = 0;
		for (int i = 0; i < lstPopulations.size(); i++) {
			sum += (float)1/lstPopulations.get(i).getIncrementalFitnessByDistance();
			if(point < sum) return i;
		}
//		return -1;
		// fitnessfunction�� ��� 0�̾ ���� ���� �� ���� ��� �������� ����.
		return (int) (Math.random() * lstPopulations.size());
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 ���� 1:39:29
	 * @date : 2010. 07. 15
	 * @return
	 * @�����̷� :
	 * @Method ���� : ���������� �����ϱ� ���� �ʿ��� �θ� ����, Roulette Wheel Selection�� ������ ���Ŀ� ���� ����.
	 */
	private int getRandomPopulationArea()
	{
		float sumOfFitnessReciprocal = 0; // fitness �Լ��� ������ ���
		for (int i = 0; i < lstPopulations.size(); i++) {
			if(lstPopulations.get(i).getFitnessByArea()!=0)
				sumOfFitnessReciprocal += (float)1/lstPopulations.get(i).getFitnessByArea();
		}
		Random r = new Random();
		float point = r.nextFloat() * sumOfFitnessReciprocal;
		float sum = 0;
		for (int i = 0; i < lstPopulations.size(); i++) {
			sum += (float)1/lstPopulations.get(i).getFitnessByArea();
			if(point<sum) return i;
		}
		// fitnessfunction�� ��� 0�̾ ���� ���� �� ���� ��� �������� ����.
		return (int) (Math.random() * lstPopulations.size());
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 ���� 1:39:11
	 * @date : 2010. 07. 15
	 * @param var1
	 * @param var2
	 * @return
	 * @�����̷� :
	 * @Method ���� : Order Crossover ������. �� �θ� �ظ� ���� ���ο� �� ����
	 */
	private MicroOrganism crossover(int var1, int var2)
	{
		// solution size ������ �ڸ����� �� index �� ���� ������ ����
		// �� ���� �� ���� ���� ū �� ����
		int chromosomeSize = lstPopulations.get(var1).getChromosomeByInt().size();
		Random r = new Random();
		int cut1 = r.nextInt(chromosomeSize / 2);
		int cut2 = r.nextInt(chromosomeSize / 2) + chromosomeSize / 2;
		
		int moduleNum = room.getM_lstModule().size(); // ����� ��
		int[] tempModuleEqNum = new int[moduleNum]; // ��⺰ ������� ��
		for (int i = 0; i < moduleNum; i++)
		{
			tempModuleEqNum[i] = moduleEqNum[i];
		}
		
		// �ڽ� ��ü
		ArrayList<Integer> chromosomeByInt = new ArrayList<Integer>(chromosomeSize);
		ArrayList<EQ> chromosomeByName = new ArrayList<EQ>(chromosomeSize);
		
		for (int i = 0; i < chromosomeSize; i++)
		{
			chromosomeByInt.add(moduleNum + 3);
		}
		// �� �� �ڸ��� ������ ������ ù��° �θ𿡼� ���
		for (int i = cut1; i < cut2; i++)
		{
			int a = lstPopulations.get(var1).getChromosomeByInt().get(i);
			chromosomeByInt.set(i, a);
			tempModuleEqNum[a]--;
		}
		
		// �ι�° �θ��� ���� �ϳ��� ������, �� �ڸ��� �� ū �ͺ��� �����Ͽ� ���� �����ϸ� ó������ �� ���ĸ� ä��� �������
		// �ڽ� ��ü�� ����Ѵ�. �� ������ �̹� ��ӵ� ������ �����Ѵ�
		// ��⿡ ���� �����ִ� ��ŭ ���ʴ�� ��ġ�ϵ�, ����� ���� ���ڶ�� ��찡 �߻��ϸ�
		// �ٸ� ����� ���� ��ġ
		for (int i = 0; i < chromosomeSize; i++)
		{
			int a = lstPopulations.get(var2).getChromosomeByInt().get(i); // var2�� ��� ���� �����Ѵ� (i�� 0���� size���� �̹Ƿ�)

			if (tempModuleEqNum[a] > 0)
			{ // ���� �̹� ��ӵ� ���� �ƴ϶��
				chromosomeByInt.set(cut2++, a); // ���������� �ϳ��� ä������
				tempModuleEqNum[a]--;
			} else
			{ // �̹� ��ġ�� ���� Module�� �߰ߵǸ�, �ٸ� Module�� ��ġ
				int k;
				do
				{
					k = new Random().nextInt(moduleNum);
				} while (tempModuleEqNum[k] <= 0);
				chromosomeByInt.set(cut2++, k);
				tempModuleEqNum[k]--;
			}
			// ���� �����ϸ� ó������ �����Ѵ�
			if (cut2 == chromosomeSize) cut2 = 0;

			// ū �ڸ����� ���� �ڸ����� �����ϸ� ��� �����ڰ� ä���� ���̹Ƿ� ������ �����Ѵ�
			if (cut2 == cut1) break;
		}
		
		chromosomeByName = eqMappingToName(chromosomeByInt, chromosomeByName);

		return new MicroOrganism(chromosomeByInt, chromosomeByName, propertiesByInt);
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 ���� 1:38:17
	 * @date : 2010. 07. 15
	 * @param chromosomeByInt
	 * @param chromosomeByName
	 * @return
	 * @�����̷� :
	 * @Method ���� : ���� ��ġ ������ ����Base�� �̷���� ����. �̸� ���� �̸� ������� Mapping �ϴ� �۾�. Cross Over, Mutate, Initialize�� �� �� ���.
	 */
	private ArrayList<EQ> eqMappingToName(ArrayList<Integer> chromosomeByInt, ArrayList<EQ> chromosomeByName)
	{
		/* 2019.07.11 �Ӵ��� review
		 * �ߺ� �ڵ� ����
		 * ��⺰�� ���� ����� üũ�ϸ鼭 ������ chromosomeByName�� ����
		 * �����ϰ� ��ġ��� �ߴµ� �������� ����-->�ڿ��� ���� ����
		 */
		int EqNum = room.getM_lstEQ().size(); // ��ü ������� ��
		int moduleNum = room.getM_lstModule().size(); // ����� ��
		int[] tempModuleEqNum = new int[moduleNum]; // ��⺰ �����
		int eqIndex = 0;
		EQ eq = null;
		chromosomeByName.clear();
		int k = 0;

		for (int j = 0; j < moduleNum; j++)
			tempModuleEqNum[j] = moduleEqNum[j];

		for (int j = 0; j < EqNum; j++)
		{ // ��⺰ ������� ���� ���� �ʴ� �ѿ��� �����ϰ� ��ġ
			k = chromosomeByInt.get(j);
			eqIndex = tempModuleEqNum[k]--;

			eq = room.getM_lstModule().get(k).getM_lstEQ().get(eqIndex - 1);
			chromosomeByName.add(eq);
		}

		return chromosomeByName;
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 ���� 1:38:17
	 * @date : 2010. 07. 15
	 * @param chromosomeByInt
	 * @param tempSolbyName
	 * @return
	 * @�����̷� :
	 * @Method ���� : ���� ��ġ ������ ����Base�� �̷���� ����. �̸� ���� �̸� ������� Mapping �ϴ� �۾�. Cross Over, Mutate, Initialize�� �� �� ���.
	 */
	private ArrayList<Integer> eqMappingToInt(ArrayList<Integer> chromosomeByInt, ArrayList<EQ> chromosomeByName)
	{
		int eqIndex = 0;
		String eqName = null;
		chromosomeByInt.clear();

		for (int i = 0; i < chromosomeByName.size(); i++)
		{
//			eqName = chromosomeByName.get(i).getMName();
			eqName = chromosomeByName.get(i).getDeviceName();
			eqIndex = propertiesByName.get(eqName);
			chromosomeByInt.add(eqIndex);
		}
		return chromosomeByInt;
	}

	// calculateDistance ������ 	
//	/**
//	 * @author jwon.cho
//	 * @param fromEQ
//	 * @param toRoom
//	 * @param totalDistance
//	 * @param freq
//	 * @return
//	 * @Method ����: EQ-Room �Ÿ� ����
//	 * zone�� ���� ��� Room�� ���� �� �� ����� ��(X => ���⼺�� ����)���� ������ ���� �Ǵ� �߾���θ� ���� �ٽ� ���ٰ� ����
//	 * eq���� ���� ���⼺�� ���� ���� �� �ִ� ������� ������, ���� ���ĺ��� ���� room �߽ɱ����� �Ÿ��� ���Ѵ�.
//	 */
//	private double calculateDistance2(EQ fromEQ, CRoom fromRoom, CRoom toRoom, double totalDistance, double freq) {
//		//subRoom�� �� �� �ִ� ��쿡 ���� ���� �߰�.. 3�� �̻��� ���� ���� ������ ���� jwon.cho
//		
//		//fromEQ Zone�� ��ġ ���⿡ ���� �ٸ��� ����ؾ� ��.
//		double distance = 0;
////System.out.println("**************");		
////System.out.println("fromEQ / fromRoom / toRoom / freq / fromEQ.isM_isEQUpperDirection() / fromEQ.isM_isBayUpperdirection()");		
////System.out.println(fromEQ.getDeviceName() + " / " + fromRoom.getM_strName() + " / " + toRoom.getM_strName() + " / " + freq + " / " + fromEQ.isM_isEQUpperDirection() + " / " + fromEQ.isM_isBayUpperdirection());
//		if(toRoom.getM_lstSubRoom().size()<2){
//			// ���񿡼� ������ ������ ���̿� ���ϴ� ���� �߽� - ��Ʈ�� ����..
//			// cf) EQUpperDirection = true;   �Ѧ�  EQUpperDirection = false;  ������
//			//                                ������                          ������
//			double x1 = fromEQ.getCoordLeft() + fromEQ.getCoordWidth()/2;
//			double y1 = 0;
//			if(fromEQ.isM_isEQUpperDirection()) y1 = fromEQ.getCoordTop() + fromEQ.getCoordLength();
//			else y1 = fromEQ.getCoordTop();
//			int fromZoneIndex = fromEQ.getZoneIndex();
//			
//			double x2 = toRoom.getM_lstSubRoom().get(0).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth()/2;
//			double y2 = toRoom.getM_lstSubRoom().get(0).getTop() + toRoom.getM_lstSubRoom().get(0).getHeight()/2;
//			int toZoneIndex = toRoom.getM_lstSubRoom().get(0).getM_nZoneIndex();
////System.out.println("fromZoneIndex / toZoneIndex");					
////System.out.println(fromZoneIndex + " / " + toZoneIndex);
//			
//			//zone�� �����Ƿ� fromRoomLeft�� toRoomLeft�� ���� ��������, �ǹ̻� ����
//			double fromRoomLeft = fromRoom.getM_lstSubRoom().get(0).getLeft();
//			double fromRoomRight = fromRoom.getM_lstSubRoom().get(0).getLeft() + fromRoom.getM_lstSubRoom().get(0).getWidth();
//			
//			double toRoomLeft = toRoom.getM_lstSubRoom().get(0).getLeft();
//			double toRoomRight = toRoom.getM_lstSubRoom().get(0).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth();
//			if(fromZoneIndex == toZoneIndex){ ////////////////////// ZONE�� ���� ���
////System.out.println("fromZoneIndex == toZoneIndex");		
//				// bay ���� ������ �ð� �������� ���ư��ٰ� ����
//				// ���̰� ���� ���ʿ� ������ ���� ����� ������, ���̰� ���� �Ʒ��ʿ� ������ ������ ����� ������ �Ѵ�.
//				   
//				double distanceX = 0;
//				if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//					if(fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomLeft) + Math.abs(toRoomLeft-x2);	
//					else distanceX = Math.abs(x1-fromRoomRight) + Math.abs(toRoomRight-x2);
//				} else {
//					if(!fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomLeft) + Math.abs(toRoomLeft-x2);	
//					else distanceX = Math.abs(x1-fromRoomRight) + Math.abs(toRoomRight-x2);
//				}
////System.out.println("************** 1 ");				
////System.out.println("x1:" + x1);					
////System.out.println("y1:" + y1);
////System.out.println("fromRoomLeft:" + fromRoomLeft);
////System.out.println("fromRoomRight:" + fromRoomRight);
////System.out.println("x2:" + x2);
////System.out.println("y2:" + y2);
////System.out.println("toRoomLeft:" + toRoomLeft);
////System.out.println("toRoomRight:" + toRoomRight);
////System.out.println("distanceX:" + distanceX);
////System.out.println("freq:" + freq);
//
//
////System.out.println("distanceX / x1 / fromRoomLeft / fromRoomRight / x2 / toRoomLeft / toRoomRight");				
////System.out.println(distanceX + " / " + x1 + " / " + fromRoomLeft + " / " + fromRoomRight + " / " + x2 + " / " + toRoomLeft + " / " + toRoomRight);
//				distance  = freq * (distanceX+Math.abs(y1-y2) * CDataSet.MICRO_ROOM_PENALTY);
//				totalDistance  += freq * (distanceX+Math.abs(y1-y2) * CDataSet.MICRO_ROOM_PENALTY);
//System.out.println("distance:" + distance);				
//				
////System.out.println("freq * (distanceX+Math.abs(y1-y2) / Math.abs(y1-y2) / y1 / y2");				
////System.out.println((freq * (distanceX+Math.abs(y1-y2)))  + " / " +  Math.abs(y1-y2)  + " / " +  y1  + " / " +  y2);
//
//			} else {
//System.out.println("fromZoneIndex != toZoneIndex");
////distance = freq * (Math.abs(x1-x2)+Math.abs(y1-y2) * CDataSet.MICRO_ZONE_PENALTY); //
//				// bay ���� ������ �ð� �������� ���ư��ٰ� ����
//				// ���̰� ���� ���ʿ� ������ ���� ����� ���� ������ �ٸ� ������ ����, ���̰� ���� �Ʒ��ʿ� ������ ������ ����� ���� ������ �ٸ� ������ ����.
//				// ���񿡼� ��������� �Ÿ��� ���ϰ� ���濡 ���� �������� �ٸ� ���� ������� �Ÿ��� ���Ѵ�.
//				double distanceX = 0;
//				if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//					if(fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1 - fromRoomLeft) + Math.abs(fromRoomLeft - x2)+Math.abs(y1 - y2);
//					else distanceX = Math.abs(x1-fromRoomRight) +  + Math.abs(fromRoomRight - x2)+Math.abs(y1 - y2);
//				} else {
//					if(!fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomRight) +  + Math.abs(fromRoomRight - x2)+Math.abs(y1 - y2);	
//					else distanceX = Math.abs(x1 - fromRoomLeft) + Math.abs(fromRoomLeft - x2)+Math.abs(y1 - y2);
//				}
//				totalDistance += freq * distanceX * CDataSet.MICRO_ZONE_PENALTY; //
//				
//				System.out.println("************** 2 ");
//				System.out.println("x1:" + x1);					
//				System.out.println("y1:" + y1);
//				System.out.println("fromRoomLeft:" + fromRoomLeft);
//				System.out.println("fromRoomRight:" + fromRoomRight);
//				System.out.println("x2:" + x2);
//				System.out.println("y2:" + y2);
//				System.out.println("toRoomLeft:" + toRoomLeft);
//				System.out.println("toRoomRight:" + toRoomRight);
//				System.out.println("distanceX:" + distanceX);
//				System.out.println("freq:" + freq);
//				System.out.println("distance:" + distanceX);	
////System.out.println("freq * (Math.abs(x1-x2)+Math.abs(y1-y2) / Math.abs(x1-x2) / x1 / x2 / Math.abs(y1-y2) / y1 / y2");				
////System.out.println((freq * (Math.abs(x1-x2)+Math.abs(y1-y2)))  + " / " +  Math.abs(x1-x2)  + " / " +  x1  + " / " +  x2  + " / " +  Math.abs(y1-y2)  + " / " +  y1  + " / " +  y2);
//			}
//		} else {
//			// ���񿡼� ������ ������ ���̿� ���ϴ� ���� �߽� - ��Ʈ�� ����..
//			// cf) EQUpperDirection = true;   �Ѧ�  EQUpperDirection = false;  ������
//			//                                ������                          ������
//			double x1 = fromEQ.getCoordLeft() + fromEQ.getCoordWidth()/2;
//			double y1 = 0;
//			if(fromEQ.isM_isEQUpperDirection()) y1 = fromEQ.getCoordTop() + fromEQ.getCoordLength();
//			else y1 = fromEQ.getCoordTop();
//			int fromZoneIndex = fromEQ.getZoneIndex();
//			
//			double x2_1 = toRoom.getM_lstSubRoom().get(0).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth()/2;
//			double y2_1 = toRoom.getM_lstSubRoom().get(0).getTop() + toRoom.getM_lstSubRoom().get(0).getHeight()/2;
//			double area_1 = toRoom.getM_lstSubRoom().get(0).getWidth() * toRoom.getM_lstSubRoom().get(0).getHeight();
//			int toZoneIndex_1 = toRoom.getM_lstSubRoom().get(0).getM_nZoneIndex();
//			
//			double x2_2 = toRoom.getM_lstSubRoom().get(1).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth()/2;
//			double y2_2 = toRoom.getM_lstSubRoom().get(1).getTop() + toRoom.getM_lstSubRoom().get(0).getHeight()/2;
//			double area_2 = toRoom.getM_lstSubRoom().get(1).getWidth() * toRoom.getM_lstSubRoom().get(1).getHeight();
//			int toZoneIndex_2 = toRoom.getM_lstSubRoom().get(1).getM_nZoneIndex();
//			
//			if(area_1 + area_2!=0){
//				//zone�� ������ ���� ���� ������� ����ϰ�, zone�� �ٸ��� x�� ������ ���밪���� ����Ѵ�.
//				double fromRoomLeft_1 = fromRoom.getM_lstSubRoom().get(0).getLeft();
//				double fromRoomRight_1 = fromRoom.getM_lstSubRoom().get(0).getLeft() + fromRoom.getM_lstSubRoom().get(0).getWidth();
//				
//				double toRoomLeft_1 = toRoom.getM_lstSubRoom().get(0).getLeft();
//				double toRoomRight_1 = toRoom.getM_lstSubRoom().get(0).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth();
//				if(fromZoneIndex == toZoneIndex_1){
//					
//					// bay ���� ������ �ð� �������� ���ư��ٰ� ����
//					// ���̰� ���� ���ʿ� ������ ���� ����� ������, ���̰� ���� �Ʒ��ʿ� ������ ������ ����� ������ �Ѵ�.
//					double distanceX_1 = 0;
//					if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//						if(fromEQ.isM_isEQUpperDirection()) distanceX_1 = Math.abs(x1-fromRoomLeft_1) + Math.abs(toRoomLeft_1-x2_1);	
//						else distanceX_1 = Math.abs(x1-fromRoomRight_1) + Math.abs(toRoomRight_1-x2_1);
//					} else {
//						if(fromEQ.isM_isEQUpperDirection()) distanceX_1 = Math.abs(x1-fromRoomLeft_1) + Math.abs(toRoomLeft_1-x2_1);	
//						else distanceX_1 = Math.abs(x1-fromRoomRight_1) + Math.abs(toRoomRight_1-x2_1);
//					}
////					double distanceX_1 = Math.min(Math.abs(x1-fromRoomLeft_1) + Math.abs(toRoomLeft_1-x2_1), Math.abs(x1-fromRoomRight_1) + Math.abs(toRoomRight_1-x2_1));
//					
//					distance  = freq * (distanceX_1+Math.abs(y1-y2_1) * CDataSet.MICRO_ROOM_PENALTY);
//					totalDistance  += freq * (distanceX_1+Math.abs(y1-y2_1) * CDataSet.MICRO_ROOM_PENALTY); 
//					
//					System.out.println("************** 3 ");
//					System.out.println("x1:" + x1);					
//					System.out.println("y1:" + y1);
//					System.out.println("fromRoomLeft_1:" + fromRoomLeft_1);
//					System.out.println("fromRoomRight_1:" + fromRoomRight_1);
//					System.out.println("x2_1:" + x2_1);
//					System.out.println("y2_1:" + y2_1);
//					System.out.println("toRoomLeft_1:" + toRoomLeft_1);
//					System.out.println("toRoomRight_1:" + toRoomRight_1);
//					System.out.println("distanceX_1:" + distanceX_1);
//					System.out.println("freq:" + freq);
//					System.out.println("distance_1:" + distanceX_1);
//				} else {
////					distance += freq * (Math.abs(x1-x2_1)+Math.abs(y1-y2_1) * CDataSet.MICRO_ZONE_PENALTY) * (area_1/(area_1 + area_2)); //
//					// bay ���� ������ �ð� �������� ���ư��ٰ� ����
//					// ���̰� ���� ���ʿ� ������ ���� ����� ���� ������ �ٸ� ������ ����, ���̰� ���� �Ʒ��ʿ� ������ ������ ����� ���� ������ �ٸ� ������ ����.
//					// ���񿡼� ��������� �Ÿ��� ���ϰ� ���濡 ���� �������� �ٸ� ���� ������� �Ÿ��� ���Ѵ�.
//					double distanceX = 0;
//					if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//						if(fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1 - fromRoomLeft_1) + Math.abs(fromRoomLeft_1 - x2_1)+Math.abs(y1 - y2_1);
//						else distanceX = Math.abs(x1-fromRoomRight_1) +  + Math.abs(fromRoomRight_1 - x2_1)+Math.abs(y1 - y2_1);
//					} else {
//						if(!fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomRight_1) +  + Math.abs(fromRoomRight_1 - x2_1)+Math.abs(y1 - y2_1);	
//						else distanceX = Math.abs(x1 - fromRoomLeft_1) + Math.abs(fromRoomLeft_1 - x2_1)+Math.abs(y1 - y2_1);
//					}
//					totalDistance += freq * distanceX * CDataSet.MICRO_ZONE_PENALTY * (area_1/(area_1 + area_2)); // 
//					
//					System.out.println("************** 4 ");
//					System.out.println("x1:" + x1);					
//					System.out.println("y1:" + y1);
//					System.out.println("fromRoomLeft_1:" + fromRoomLeft_1);
//					System.out.println("fromRoomRight_1:" + fromRoomRight_1);
//					System.out.println("x2_1:" + x2_1);
//					System.out.println("y2_1:" + y2_1);
//					System.out.println("toRoomLeft_1:" + toRoomLeft_1);
//					System.out.println("toRoomRight_1:" + toRoomRight_1);
//					System.out.println("distanceX:" + distanceX);
//					System.out.println("freq:" + freq);
//					System.out.println("distance:" + distanceX);
//				}	
//				double fromRoomLeft_2 = fromRoom.getM_lstSubRoom().get(0).getLeft();
//				double fromRoomRight_2 = fromRoom.getM_lstSubRoom().get(0).getLeft() + fromRoom.getM_lstSubRoom().get(0).getWidth();
//				
//				double toRoomLeft_2 = toRoom.getM_lstSubRoom().get(1).getLeft();
//				double toRoomRight_2 = toRoom.getM_lstSubRoom().get(1).getLeft() + toRoom.getM_lstSubRoom().get(1).getWidth();
//				if(fromZoneIndex == toZoneIndex_2){
//					// bay ���� ������ �ð� �������� ���ư��ٰ� ����
//					// ���̰� ���� ���ʿ� ������ ���� ����� ������, ���̰� ���� �Ʒ��ʿ� ������ ������ ����� ������ �Ѵ�.
//					double distanceX_2 = 0;
//					if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//						if(fromEQ.isM_isEQUpperDirection()) distanceX_2 = Math.abs(x1-fromRoomLeft_2) + Math.abs(toRoomLeft_2-x2_2);	
//						else distanceX_2 = Math.abs(x1-fromRoomRight_2) + Math.abs(toRoomRight_2-x2_2);
//					} else {
//						if(fromEQ.isM_isEQUpperDirection()) distanceX_2 = Math.abs(x1-fromRoomLeft_2) + Math.abs(toRoomLeft_2-x2_2);	
//						else distanceX_2 = Math.abs(x1-fromRoomRight_2) + Math.abs(toRoomRight_2-x2_2);
//					}
//					
//					System.out.println("************** 5 ");
//					System.out.println("x1:" + x1);					
//					System.out.println("y1:" + y1);
//					System.out.println("fromRoomLeft_2:" + fromRoomLeft_2);
//					System.out.println("fromRoomRight_2:" + fromRoomRight_2);
//					System.out.println("x2_2:" + x2_2);
//					System.out.println("y2_2:" + y2_2);
//					System.out.println("toRoomLeft_2:" + toRoomLeft_2);
//					System.out.println("toRoomRight_2:" + toRoomRight_2);
//					System.out.println("distanceX_2:" + distanceX_2);
//					System.out.println("freq:" + freq);
//					System.out.println("distance_2:" + distanceX_2);
////					double distanceX_2 = Math.min(Math.abs(x1-fromRoomLeft_2) + Math.abs(toRoomLeft_2-x2_2), Math.abs(x1-fromRoomRight_2) + Math.abs(toRoomRight_2-x2_2));
////					distance  += freq * (distanceX_2+Math.abs(y1-y2_2) * CDataSet.MICRO_ROOM_PENALTY);
//					totalDistance  += freq * (distanceX_2+Math.abs(y1-y2_2) * CDataSet.MICRO_ROOM_PENALTY); 
//				} else {
////					distance += freq * (Math.abs(x1-x2_2)+Math.abs(y1-y2_2) * CDataSet.MICRO_ZONE_PENALTY) * (area_2/(area_1 + area_2)); //
//					double distanceX = 0;
//					if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//						if(fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1 - fromRoomLeft_2) + Math.abs(fromRoomLeft_2 - x2_2)+Math.abs(y1 - y2_2);
//						else distanceX = Math.abs(x1-fromRoomRight_2) +  + Math.abs(fromRoomRight_2 - x2_2)+Math.abs(y1 - y2_2);
//					} else {
//						if(!fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomRight_2) +  + Math.abs(fromRoomRight_2 - x2_2)+Math.abs(y1 - y2_2);	
//						else distanceX = Math.abs(x1 - fromRoomLeft_2) + Math.abs(fromRoomLeft_2 - x2_2)+Math.abs(y1 - y2_2);
//					}
//					totalDistance += freq * distanceX * CDataSet.MICRO_ZONE_PENALTY * (area_2/(area_1 + area_2)); //
//					
//					System.out.println("************** 6 ");
//					System.out.println("x1:" + x1);					
//					System.out.println("y1:" + y1);
//					System.out.println("fromRoomLeft_2:" + fromRoomLeft_2);
//					System.out.println("fromRoomRight_2:" + fromRoomRight_2);
//					System.out.println("x2_2:" + x2_2);
//					System.out.println("y2_2:" + y2_2);
//					System.out.println("toRoomLeft_2:" + toRoomLeft_2);
//					System.out.println("toRoomRight_2:" + toRoomRight_2);
//					System.out.println("distanceX:" + distanceX);
//					System.out.println("freq:" + freq);
//					System.out.println("distance:" +distanceX);
//				}
//			}
//		}
////		System.out.printlndistance:"+distance);
//		return totalDistance;
//	}
}
